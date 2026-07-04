import { Component, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService, ConfirmationService, ConfirmEventType } from 'primeng/api';
import { debounceTime, distinctUntilChanged, finalize, Subscription } from 'rxjs';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';

import { ContratCadreService } from '../../services/contrat-cadre.service';
import { ContratCadreResponse, StatutContratCadre } from '../../dtos/ContratCadreResponse';
import { ContratCadreRequest } from '../../dtos/ContratCadreRequest';
import { ContratCadreUpdate } from '../../dtos/ContratCadreUpdate';
import { ContratCadreSearch } from '../../dtos/ContratCadreSearch';
import { AuthService } from '../../../../../auth/services/auth.service';
import { OrganismeLeasingService } from '../../../organismesLeasing/services/organisme-leasing.service';
import { OrganismeLeasingResponse } from '../../../organismesLeasing/dtos/OrganismeLeasingResponse';
import { PageResponse } from '../../../../global/types/PageResponse';
import { toLocalDateString, parseLocalDate } from '../../../../global/types/date.utils';

@Component({
  selector: 'app-contrat-cadre-list',
  templateUrl: './contrat-cadre-list.component.html',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    TagModule,
    TooltipModule,
    RippleModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class ContratCadreListComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt: Table | undefined;

  private readonly contratCadreService     = inject(ContratCadreService);
  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly fb                      = inject(FormBuilder);
  private readonly messageService          = inject(MessageService);
  private readonly confirmationService     = inject(ConfirmationService);
  private readonly authService             = inject(AuthService);
  private readonly route                   = inject(ActivatedRoute);

  // --- Liste ---
  contratsCadres: ContratCadreResponse[] = [];
  totalRecords = 0;
  loading      = false;

  private lastLazyEvent: TableLazyLoadEvent = {
    first: 0, rows: 10, sortField: 'id', sortOrder: -1, filters: {}
  };

  // --- Filtres ---
  searchForm!: FormGroup;
  organismeOptions: { label: string; value: number }[] = [];
  statutOptions = [
    { label: 'ACTIF',   value: 'ACTIF'   },
    { label: 'EXPIRÉ',  value: 'EXPIRE'  },
    { label: 'ARCHIVÉ', value: 'ARCHIVE' }
  ];

  // --- Dialogue création ---
  showCreateDialog = false;
  createForm!: FormGroup;
  submittingCreate = false;

  // --- Dialogue édition ---
  showEditDialog   = false;
  editForm!: FormGroup;
  submittingEdit   = false;
  selectedCadreId: number | null = null;

  private subs = new Subscription();

  ngOnInit(): void {
    const organismeIdParam = this.route.snapshot.queryParamMap.get('organismeId');
    const initialOrganismeId = organismeIdParam ? +organismeIdParam : null;

    this.searchForm = this.fb.group({
      organismeId: [initialOrganismeId],
      statut:      [null]
    });

    this.createForm = this.fb.group({
      organismeId: [null, Validators.required],
      plafond:     [null, [Validators.required, Validators.min(0.01)]],
      dateDebut:   [null, Validators.required],
      dateFin:     [null, Validators.required]
    });

    this.editForm = this.fb.group({
      plafond:   [null, [Validators.required, Validators.min(0.01)]],
      dateDebut: [null, Validators.required],
      dateFin:   [null, Validators.required]
    });

    this.loadOrganismeOptions();

    this.subs.add(
      this.searchForm.valueChanges
        .pipe(debounceTime(300), distinctUntilChanged())
        .subscribe(() => this.onSearch())
    );

    this.load(this.lastLazyEvent);
  }

  // ---------------------------------------------------------------
  // Chargement de la liste
  // ---------------------------------------------------------------
  load(event: TableLazyLoadEvent): void {
    this.loading       = true;
    this.lastLazyEvent = event;

    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;

    let sort = 'id,desc';
    if (event.sortField) {
      const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sort = `${field},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const criteria: ContratCadreSearch = {
      organismeId: this.searchForm.get('organismeId')?.value ?? undefined,
      statut:      this.searchForm.get('statut')?.value ?? undefined
    };

    this.subs.add(
      this.contratCadreService.search(criteria, page, size, sort)
        .pipe(finalize(() => this.loading = false))
        .subscribe({
          next: (resp: PageResponse<ContratCadreResponse>) => {
            this.contratsCadres = resp.content;
            this.totalRecords   = resp.page.totalElements;
          }
        })
    );
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.load(this.dt.createLazyLoadMetadata());
    } else {
      this.load({ ...this.lastLazyEvent, first: 0 });
    }
  }

  clearFilters(): void {
    this.searchForm.reset({ organismeId: null, statut: null });
    if (this.dt) { this.dt.clear(); this.dt.first = 0; }
  }

  // ---------------------------------------------------------------
  // Dropdowns
  // ---------------------------------------------------------------
  private loadOrganismeOptions(): void {
    this.subs.add(
      this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000).subscribe({
        next: (resp: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeOptions = resp.content.map(o => ({ label: o.nom, value: o.id }));
        }
      })
    );
  }

  // ---------------------------------------------------------------
  // Dialogue création
  // ---------------------------------------------------------------
  openCreateDialog(): void {
    this.createForm.reset();
    this.showCreateDialog = true;
  }

  onSubmitCreate(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;

    const v = this.createForm.value;
    const request: ContratCadreRequest = {
      organismeId: v.organismeId,
      plafond:     v.plafond,
      dateDebut:   toLocalDateString(v.dateDebut)!,
      dateFin:     toLocalDateString(v.dateFin)!
    };

    this.submittingCreate = true;
    this.subs.add(
      this.contratCadreService.create(request)
        .pipe(finalize(() => this.submittingCreate = false))
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat cadre créé.' });
            this.showCreateDialog = false;
            this.onSearch();
          }
        })
    );
  }

  // ---------------------------------------------------------------
  // Dialogue édition
  // ---------------------------------------------------------------
  openEditDialog(cadre: ContratCadreResponse): void {
    this.selectedCadreId = cadre.id;
    this.editForm.setValue({
      plafond:   cadre.plafond,
      dateDebut: parseLocalDate(cadre.dateDebut),
      dateFin:   parseLocalDate(cadre.dateFin)
    });
    this.showEditDialog = true;
  }

  onSubmitEdit(): void {
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid || this.selectedCadreId === null) return;

    const v = this.editForm.value;
    const payload: ContratCadreUpdate = {
      plafond:   v.plafond,
      dateDebut: toLocalDateString(v.dateDebut)!,
      dateFin:   toLocalDateString(v.dateFin)!
    };

    this.submittingEdit = true;
    this.subs.add(
      this.contratCadreService.update(this.selectedCadreId, payload)
        .pipe(finalize(() => this.submittingEdit = false))
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat cadre mis à jour.' });
            this.showEditDialog = false;
            this.selectedCadreId = null;
            this.onSearch();
          }
        })
    );
  }

  // ---------------------------------------------------------------
  // Archivage
  // ---------------------------------------------------------------
  confirmArchiver(cadre: ContratCadreResponse): void {
    this.confirmationService.confirm({
      message: `Archiver le contrat cadre de "${cadre.organismeNom}" (plafond ${cadre.plafond.toLocaleString('fr-FR')} DH) ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui, archiver',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => this.archiver(cadre.id),
      reject: (type: ConfirmEventType) => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Archivage annulé.' });
      }
    });
  }

  archiver(id: number): void {
    this.subs.add(
      this.contratCadreService.archiver(id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat cadre archivé.' });
          this.onSearch();
        }
      })
    );
  }

  // ---------------------------------------------------------------
  // RBAC
  // ---------------------------------------------------------------
  /** Finance (MEMBER ou DIRECTOR) ou SYSTEM_ADMIN : peut créer, modifier, archiver. */
  canModify(): boolean {
    return this.authService.isSystemAdmin() || this.authService.belongsToDepartment('Finance');
  }

  // ---------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------
  statutSeverity(statut: StatutContratCadre): 'success' | 'warning' | 'secondary' {
    switch (statut) {
      case 'ACTIF':   return 'success';
      case 'EXPIRE':  return 'warning';
      case 'ARCHIVE': return 'secondary';
    }
  }

  get fc()  { return this.createForm.controls; }
  get fe()  { return this.editForm.controls; }

  ngOnDestroy(): void { this.subs.unsubscribe(); }
}
