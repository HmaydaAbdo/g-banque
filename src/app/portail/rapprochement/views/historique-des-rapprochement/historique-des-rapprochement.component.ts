import {Component, OnInit, ViewChild, inject} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {catchError, debounceTime, distinctUntilChanged, finalize} from 'rxjs/operators';
import { Router} from '@angular/router';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {DropdownModule} from "primeng/dropdown";
import {RapprochementResponse} from "../../dtos/RapprochementResponse";
import {RapprochementService} from "../../service/rapprochement-service.service";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {RapprochementSearchDto} from "../../dtos/RapprochementSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {throwError} from "rxjs";
import {ToolbarModule} from "primeng/toolbar";
import {DialogModule} from "primeng/dialog";
import {SelectButtonModule} from "primeng/selectbutton";
import {StatutRapprochement} from "../../enums/StatutRapprochement";



@Component({
  selector: 'app-historique-des-rapprochement',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    TagModule,
    TooltipModule,
    BreadcrumbModule,
    DropdownModule,
    ToolbarModule,
    DialogModule,
    SelectButtonModule
  ],
  templateUrl: './historique-des-rapprochement.component.html',
  styleUrls: ['./historique-des-rapprochement.component.scss'],
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class HistoriqueDesRapprochementComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  rapprochements!: RapprochementResponse[];
  items!: MenuItem[] ;
  totalRecords!: number
  loading: boolean = false;
  first: number | undefined = 0;
  rows: number | undefined = 10;
  sortField: string | string[] = 'dateRapprochement';
  sortOrder: number = -1;
  statusOptions = [
    { label: 'Tous les statuts', value: null },
    { label: 'En cours', value: StatutRapprochement.EN_COURS },
    { label: 'Clôture provisoire', value: StatutRapprochement.CLOTURE_PROVISOIRE },
    { label: 'Clôturé définitivement', value: StatutRapprochement.CLOTURE_DEFINITIVE }
  ];
  searchForm!: FormGroup;

  banques: { id: number; nom: string }[] = [];
  loadingBanques: boolean = false;

  loadingReport: { [key: number]: boolean } = {};

  showReportTypeModal: boolean = false;
  reportTypeControl = new FormControl<'FINANCE' | 'COMPTABILITE' | null>(null);
  pendingRapprochement: RapprochementResponse | null = null;
  reportTypeOptions = [
    {
      label: 'Finance',
      value: 'FINANCE',
      icon: 'pi pi-chart-bar',
      description: 'Inclut TVA et date d\'émission'
    },
    {
      label: 'Comptabilité',
      value: 'COMPTABILITE',
      icon: 'pi pi-book',
      description: 'Sans TVA ni date d\'émission'
    }
  ];

  private readonly rapprochementService = inject(RapprochementService);
  private readonly messageService = inject(MessageService);
  private readonly banqueService = inject(BanqueService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-history', label: 'Historique des Rapprochements',route: '/portail/rapprochement/historique' }
    ];
    this.initSearchForm();
    this.searchForm.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.onSearch();
      });

     this.loadBanques({ first: 0, rows: 10 });
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      dateRapprochement: [''],
      statutRapprochement: [null]
    });
  }

  loadRapprochements(event: TableLazyLoadEvent = { first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder, filters: {} }): void {
    this.loading = true;
    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = (first as number) / (rows as number);

    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    // Récupérer les critères de recherche du formulaire
    const searchCriteria: RapprochementSearchDto = this.searchForm.value;

    this.rapprochementService.searchRapprochements(searchCriteria, page, rows as number)
      .subscribe({
        next: (response: PageResponse<RapprochementResponse>) => {
          this.rapprochements = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }


  loadBanques(event: any) {
    this.loadingBanques = true;

    const page = event.first / event.rows || 0;
    const size = event.rows || 10000;

    const criteria = { nom: event.filter }; // filtre par nom si tapé

    this.banqueService.getAllBanques(criteria, page, size).subscribe({
      next: (data) => {
        this.banques = data.content.map(b => ({ id: b.id, nom: b.nom }));
        this.loadingBanques = false;
      },
      error: () => {
        this.loadingBanques = false;
      }
    });
  }

  onSearch(): void {
    this.first = 0;
    if (this.dt) {
      // Déclenche un nouveau lazy load avec les nouveaux filtres et réinitialise la pagination
      this.dt.reset();
    } else {
      this.loadRapprochements();
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      banqueId: null,
      dateRapprochement: '',
      statutRapprochement: null
    });
    this.first = 0;
    if (this.dt) {
      // Réinitialise la table et recharge
      this.dt.clear();
      this.loadRapprochements(this.dt.createLazyLoadMetadata());
    } else {
      this.loadRapprochements();
    }
  }

  /**
   * Navigue vers la page de détails d'un rapprochement (où les transactions sont paginées)
   */
  viewRapprochementDetails(rapprochementId: number): void {
    this.router.navigate(['/portail/rapprochement/details', rapprochementId]);
  }

  downloadRapprochementReport(rapprochement: RapprochementResponse): void {
    if (!rapprochement.id) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Alerte',
        detail: 'ID du rapprochement manquant.',
        life: 3000
      });
      return;
    }
    // Ouvre la modal et mémorise le rapprochement en attente
    this.pendingRapprochement = rapprochement;
    this.reportTypeControl.setValue('FINANCE'); // Finance par défaut
    this.showReportTypeModal = true;
  }

// Ajouter cette méthode
  confirmDownload(): void {
    if (!this.pendingRapprochement || !this.reportTypeControl.value) return;

    const rapprochement = this.pendingRapprochement;
    const reportType = this.reportTypeControl.value;

    this.showReportTypeModal = false;
    this.loadingReport[rapprochement.id] = true;

    this.rapprochementService.downloadReport(rapprochement.id, reportType)
      .pipe(
        catchError(() => {
          return throwError(() => new Error('Download failed'));
        }),
        finalize(() => {
          this.loadingReport[rapprochement.id] = false;
          this.pendingRapprochement = null;
          this.reportTypeControl.setValue(null);
        })
      )
      .subscribe((response: Blob) => {
        const fileURL = URL.createObjectURL(response);
        const link = document.createElement('a');
        link.href = fileURL;
        link.download = `Rapport_Rapprochement_${rapprochement.banqueNom}_${rapprochement.dateRapprochement}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(fileURL);

        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Rapport ${reportType} téléchargé avec succès.`,
          life: 3000
        });
      });
  }}
