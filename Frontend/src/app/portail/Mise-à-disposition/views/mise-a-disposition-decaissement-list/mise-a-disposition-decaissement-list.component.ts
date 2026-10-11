import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';

// PrimeNG modules
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {InputTextModule} from 'primeng/inputtext';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {ToolbarModule} from 'primeng/toolbar';
import {ConfirmationService, MenuItem, MessageService, SelectItem} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {DropdownModule} from 'primeng/dropdown';
import {InputSwitch, InputSwitchModule} from "primeng/inputswitch";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLink, RouterLinkActive} from "@angular/router";
import {CalendarModule} from "primeng/calendar";

// Project-specific imports
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {AuthService} from "../../../../auth/services/auth.service";
import {MiseADispositionResponseDto} from "../../dtos/MiseADispositionResponseDto";
import {MiseADispositionService} from "../../service/MiseADispositionService";
import {MiseADispositionSearchDto} from "../../dtos/MiseADispositionSearchDto";
import {StatutMoyenPaiement} from "../../../global/enums/StatutMoyenPaiement";


@Component({
  selector: 'app-mise-a-disposition-decaissement-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    InputTextModule,
    TableModule,
    TagModule,
    TooltipModule,
    RippleModule,
    ConfirmDialogModule,
    DropdownModule,
    InputSwitchModule,
    FormsModule,
    BreadcrumbModule,
    RouterLinkActive,
    RouterLink,
    CalendarModule
  ],
  templateUrl: './mise-a-disposition-decaissement-list.component.html',
  styleUrl: './mise-a-disposition-decaissement-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class MiseADispositionDecaissementListComponent implements OnInit, OnDestroy {

  protected readonly StatutMoyenPaiement = StatutMoyenPaiement;

  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Toutes les mises à disposition', value: null},
    {label: 'En cours', value: StatutMoyenPaiement.ENCOURS},
    {label: 'Réglées', value: StatutMoyenPaiement.REGLE},
    {label: 'Annulées', value: StatutMoyenPaiement.ANNULE}
  ];

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private miseADispositionService: MiseADispositionService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/Mise-a-disposition/decaissement', label: 'Les mises à disposition (décaissements)'},
    ];
    this.isAdmin = this.authService.isSystemAdmin();
    this.initSearchForm();
    this.loadBanques();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numero: [''],
      beneficiaire: [''],
      cinBeneficiaire: [''],
      banqueName: [null],
      statut: [null],
      dateEmissionFrom: [null],
      dateEmissionTo: [null],
      dateOperationFrom: [null],
      dateOperationTo: [null],
      dateValeurFrom: [null],
      dateValeurTo: [null]
    });
    const sub = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(sub);
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEmissionFrom || v.dateEmissionTo) count++;
    if (v.dateOperationFrom || v.dateOperationTo) count++;
    if (v.dateValeurFrom || v.dateValeurTo) count++;
    return count;
  }

  loadMisesADisposition(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: MiseADispositionSearchDto = this.searchForm.getRawValue();

    this.miseADispositionService.getDecaissementsForMiseADisposition(searchCriteria, currentPage, pageSize, sort).subscribe({
      next: (page: PageResponse<MiseADispositionResponseDto>) => {
        this.misesADisposition = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
    }
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(b => ({label: b.nom, value: b.nom}));
        this.banqueOptions.unshift({label: 'Toutes les banques', value: null});
      },
      error: () => {}
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      numero: '', beneficiaire: '', cinBeneficiaire: '', banqueName: null, statut: null,
      dateEmissionFrom: null, dateEmissionTo: null,
      dateOperationFrom: null, dateOperationTo: null,
      dateValeurFrom: null, dateValeurTo: null
    });
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
    }
  }

  getStatutSeverity(statut: StatutMoyenPaiement): 'success' | 'warning' | 'danger' | 'info' {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'success';
      case StatutMoyenPaiement.REGLE:   return 'warning';
      case StatutMoyenPaiement.ANNULE:  return 'danger';
      default: return 'info';
    }
  }

  getStatutLabel(statut: StatutMoyenPaiement): string {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'En cours';
      case StatutMoyenPaiement.REGLE:   return 'Réglée';
      case StatutMoyenPaiement.ANNULE:  return 'Annulée';
      default: return statut;
    }
  }

  confirmToggleStatus(mad: MiseADispositionResponseDto, sw: InputSwitch): void {
    // Le switch bascule visuellement au clic : on le recale sur le statut réel si l'opération n'aboutit pas.
    const resync = () => sw.writeValue(mad.statut === StatutMoyenPaiement.ENCOURS);
    const toRegle = mad.statut === StatutMoyenPaiement.ENCOURS;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir ${toRegle ? 'régler' : 'remettre en cours'} la mise à disposition N° ${mad.numero} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: toRegle ? 'p-button-warning' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.toggleMiseADispositionStatus(mad.id, resync),
      reject: resync
    });
  }

  toggleMiseADispositionStatus(id: number, onError: () => void): void {
    this.miseADispositionService.toggleMiseADispositionStatus(id).subscribe({
      next: (updated) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Mise à disposition N° ${updated.numero} — statut : ${this.getStatutLabel(updated.statut)}`
        });
        if (this.dt) this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour de la mise à disposition:', err);
        onError();
      }
    });
  }

  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: MiseADispositionSearchDto = this.searchForm.getRawValue();
    this.miseADispositionService.exportDecaissementsPdf(searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mises_a_disposition_decaissements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }
}
