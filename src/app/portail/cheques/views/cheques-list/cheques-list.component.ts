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

// Project-specific imports
import {ChequeResponseDto} from "../../dtos/ChequeResponseDto";
import {ChequeService} from "../../services/ChequeService";
import {ChequeSearchDto} from "../../dtos/ChequeSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {InputSwitchModule} from "primeng/inputswitch";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive} from "@angular/router";


@Component({
  selector: 'app-cheques-list',
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
    RouterLinkActive
  ],
  templateUrl: './cheques-list.component.html',
  styleUrl: './cheques-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class ChequesListComponent implements OnInit, OnDestroy {
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
      { icon: 'pi pi-list', route: '/portail/Chèques', label:"Les Cheques" },
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
      numero: [''],
      beneficiaire: [''],
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

    const searchCriteria: ChequeSearchDto = {
      numero: this.searchForm.get('numero')?.value,
      beneficiaire: this.searchForm.get('beneficiaire')?.value,
      nomBanque: this.searchForm.get('nomBanque')?.value,
      active: this.searchForm.get('active')?.value
    };

    this.chequeService.searchCheques(
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
    this.searchForm.reset({
      numero: '',
      beneficiaire: '',
      nomBanque: null,
      active: null
    });

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
    const confirmationMessage = `Êtes-vous sûr de vouloir ${cheque.active ? 'désactiver' : 'activer'} le chèque N° ${cheque.numero}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${cheque.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: cheque.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleChequeStatus(cheque.numero);
      }
    });
  }

  /**
   * Calls the service to toggle the cheque's status.
   * @param numero The numero of the cheque.
   */
  toggleChequeStatus(numero: string): void {
    this.chequeService.toggleChequeStatus(numero).subscribe({
      next: (updatedCheque) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Chèque N° ${updatedCheque.numero} ${updatedCheque.active ? 'activé' : 'désactivé'} avec succès.`
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
}
