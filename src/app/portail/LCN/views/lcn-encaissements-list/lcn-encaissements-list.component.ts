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
import {LcnResponseDTO} from "../../dtos/LcnResponseDTO";
import {Subscription} from "rxjs";
import {LcnService} from "../../service/LcnService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {LcnSearchDTO} from "../../dtos/LcnSearchDTO";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {Ripple} from "primeng/ripple";

@Component({
  selector: 'app-lcn-encaissements-list',
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
    Ripple
  ],
  templateUrl: './lcn-encaissements-list.component.html',
  styleUrl: './lcn-encaissements-list.component.scss'
})
export class LcnEncaissementsListComponent implements OnInit, OnDestroy {
  lcns: LcnResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  // property to store the list of banks for the dropdown.
  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Tous les LCN', value: null},
    {label: 'Les LCN en cours', value: true},
    {label: 'Les LCN archivés', value: false}
  ];

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEcheance', // Default sort field
    sortOrder: -1, // Sort descending by default
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private lcnService: LcnService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {
  }

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil"},
      {icon: 'pi pi-list', route: '/portail/LCN/encaissements', label: " LCN (encaissements)"},
    ]
    this.isAdmin = this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
    this.loadLcns(this.lastLazyLoadEvent);
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
   * Loads LCNs based on lazy load event and form filters.
   * @param event The PrimeNG lazy load event.
   */
  loadLcns(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'dateEcheance,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: LcnSearchDTO = {
      numero: this.searchForm.get('numero')?.value,
      beneficiaire: this.searchForm.get('beneficiaire')?.value,
      nomBanque: this.searchForm.get('nomBanque')?.value,
      active: this.searchForm.get('active')?.value
    };

    this.lcnService.getEncaissementLcns(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<LcnResponseDTO>) => {
        this.lcns = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des LCNs:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des LCNs.'
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
      this.loadLcns(this.dt.createLazyLoadMetadata());
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
          this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to load banks.'});
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
      this.loadLcns(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Confirms and toggles the status of an LCN.
   * @param lcn The LCN to toggle.
   */
  confirmToggleStatus(lcn: LcnResponseDTO): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${lcn.active ? 'désactiver' : 'activer'} le LCN N° ${lcn.numero}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${lcn.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: lcn.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleLcnStatus(lcn.id);
      }
    });
  }

  /**
   * Calls the service to toggle the LCN's status.
   * @param numero The numero of the LCN.
   */
  toggleLcnStatus(id: number): void {
    this.lcnService.toggleLcnStatus(id).subscribe({
      next: (updatedLcn) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `LCN N° ${updatedLcn.numero} ${updatedLcn.active ? 'activé' : 'désactivé'} avec succès.`
        });
        // Reload data from the server to reflect the change
        if (this.dt) {
          this.loadLcns(this.dt.createLazyLoadMetadata());
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du LCN:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `Échec de la mise à jour du LCN.`
        });
      }
    });
  }

  deleteLcn(lcn: LcnResponseDTO): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer ce Lcn "<strong>${lcn.id}</strong>" ?</p>
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
        this.lcnService.deleteLcn(lcn.id)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Le lcn a été supprimé avec succès.', life: 3000 });
              this.loadLcns(this.lastLazyLoadEvent)
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression du lcn:', error);
              const errorMessage = error.error.detail || 'Échec de la suppression du lcn.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }
}
