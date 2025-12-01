import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DatePipe, DecimalPipe, NgClass, NgIf} from "@angular/common";
import {DropdownModule} from "primeng/dropdown";
import {InputSwitchModule} from "primeng/inputswitch";
import {InputTextModule} from "primeng/inputtext";
import {PaginatorModule} from "primeng/paginator";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate, SelectItem} from "primeng/api";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {RouterLinkActive} from "@angular/router";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {ChequeResponseDto} from "../../dtos/ChequeResponseDto";
import {Subscription} from "rxjs";
import {ChequeService} from "../../services/ChequeService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {ChequeSearchDto} from "../../dtos/ChequeSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {Ripple} from "primeng/ripple";
import {CalendarModule} from "primeng/calendar";

@Component({
  selector: 'app-cheques-encaissements-list',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    InputSwitchModule,
    InputTextModule,
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ReactiveFormsModule,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    NgClass,
    Ripple,
    CalendarModule
  ],
  templateUrl: './cheques-encaissements-list.component.html',
  styleUrl: './cheques-encaissements-list.component.scss'
})
export class ChequesEncaissementsListComponent implements OnInit, OnDestroy {
  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  isAdmin: boolean=false ;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  // property to store the list of banks for the dropdown.
  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Tous les cheque', value: null},
    {label: 'Les cheques en cours', value: true},
    {label: 'Les cheques archivés', value: false}
  ];

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEmission', // Default sort field
    sortOrder: -1, // Sort descending by default
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private chequeService: ChequeService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService:AuthService
  ) {
  }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/Cheques/encaissements', label:"Les Cheques ( encaissements )" },
    ]
    this.isAdmin=this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
    this.loadCheques(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the search form with all filter controls.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      dateEmission: [null],
      montant: [null],
      numero: [''],
      emetteur: [''],
      nomBanque: [null],
      active: [null]
    });

    const searchSubscription = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(searchSubscription);
  }

  /**
   * Loads cheques based on lazy load event and form filters.
   * @param event The PrimeNG lazy load event.
   */
  loadCheques(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: ChequeSearchDto = this.searchForm.getRawValue();

    this.chequeService.getEncaissementCheques(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<ChequeResponseDto>) => {
        this.cheques = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des chèques:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des chèques.'
        });
      }
    });
  }

  /**
   * Triggers a new search from the first page.
   */
  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadCheques(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Loads the list of banks for the dropdown filter.
   */
  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banqueOptions = response.content.map(banque => ({
            label: banque.nom,
            value: banque.nom
          }));
          this.banqueOptions.unshift({label: 'Toutes les banques', value: null});
        },
        error: (error) => {
          console.error('Error loading bank options:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load banks.' });
        }
      });
  }
  /**
   * Clears all search filters and reloads the table.
   */
  clearSearch(): void {
    this.searchForm.reset();

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadCheques(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Confirms and toggles the status of a cheque.
   * @param cheque The cheque to toggle.
   */
  confirmToggleStatus(cheque: ChequeResponseDto): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${cheque.active ? 'désactiver' : 'activer'} le chèque id ${cheque.id}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${cheque.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: cheque.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleChequeStatus(cheque.id);
      }
    });
  }


  toggleChequeStatus(id: number): void {
    this.chequeService.toggleChequeStatus(id).subscribe({
      next: (updatedCheque) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Chèque Id° ${updatedCheque.id} ${updatedCheque.active ? 'activé' : 'désactivé'} avec succès.`
        });
        // Reload data from the server to reflect the change
        if (this.dt) {
          this.loadCheques(this.dt.createLazyLoadMetadata());
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du chèque:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `Échec de la mise à jour du chèque.`
        });
      }
    });
  }

  deleteCheque(cheque: ChequeResponseDto): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer ce chèque "<strong>${cheque.id}</strong>" ?</p>
        <p style="color: #ef4444; font-weight: bold;">Cette action est irréversible.</p>
    </div>
    `;

    this.confirmationService.confirm({
      message: confirmationMessageHtml,
      header: 'Confirmation de Suppression',
      icon: 'none',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.chequeService.deleteCheque(cheque.id)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Le cheque a été supprimé avec succès.', life: 3000 });
              this.loadCheques(this.lastLazyLoadEvent)
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression du cheque:', error);
              const errorMessage = error.error.detail || 'Échec de la suppression du cheque.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

}
