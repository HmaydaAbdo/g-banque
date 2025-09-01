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
import {InputSwitchModule} from "primeng/inputswitch";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive} from "@angular/router";

// Project-specific imports
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {MiseADispositionResponseDto} from "../../dtos/MiseADispositionResponseDto";
import {MiseADispositionService} from "../../service/MiseADispositionService";
import {MiseADispositionSearchDto} from "../../dtos/MiseADispositionSearchDto";


@Component({
  selector: 'app-mise-a-disposition-list',
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
  templateUrl: './mise-a-disposition-list.component.html',
  styleUrl: './mise-a-disposition-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class MiseADispositionListComponent implements OnInit, OnDestroy {
  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Toutes les mises à disposition', value: null},
    {label: 'Les mises à disposition encours', value: true},
    {label: 'Les mises à disposition archivées', value: false}
  ];

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEmission',
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
      {icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil"},
      {icon: 'pi pi-list', route: '/portail/mises-a-disposition', label: "Les mises à disposition"},
    ];
    this.isAdmin = this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
    this.loadMisesADisposition(this.lastLazyLoadEvent);
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
      cinBeneficiaire: [''],
      banqueName: [null],
      active: [null]
    });

    const searchSubscription = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(searchSubscription);
  }

  /**
   * Loads mises à disposition based on lazy load event and form filters.
   * @param event The PrimeNG lazy load event.
   */
  loadMisesADisposition(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: MiseADispositionSearchDto = {
      numero: this.searchForm.get('numero')?.value,
      beneficiaire: this.searchForm.get('beneficiaire')?.value,
      cinBeneficiaire: this.searchForm.get('cinBeneficiaire')?.value,
      banqueName: this.searchForm.get('banqueName')?.value,
      active: this.searchForm.get('active')?.value
    };

    this.miseADispositionService.searchMisesADisposition(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<MiseADispositionResponseDto>) => {
        this.misesADisposition = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des mises à disposition:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des mises à disposition.'
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
      this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
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
      cinBeneficiaire: '',
      banqueName: null,
      active: null
    });

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Confirms and toggles the status of a mise à disposition.
   * @param miseADisposition The mise à disposition to toggle.
   */
  confirmToggleStatus(miseADisposition: MiseADispositionResponseDto): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${miseADisposition.active ? 'désactiver' : 'activer'} la mise à disposition N° ${miseADisposition.numero}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${miseADisposition.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: miseADisposition.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleMiseADispositionStatus(miseADisposition.numero);
      }
    });
  }

  /**
   * Calls the service to toggle the mise à disposition's status.
   * @param numero The numero of the mise à disposition.
   */
  toggleMiseADispositionStatus(numero: string): void {
    this.miseADispositionService.toggleMiseADispositionStatus(numero).subscribe({
      next: (updatedMiseADisposition) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Mise à disposition N° ${updatedMiseADisposition.numero} ${updatedMiseADisposition.active ? 'activée' : 'désactivée'} avec succès.`
        });
        // Reload data from the server to reflect the change
        if (this.dt) {
          this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour de la mise à disposition:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `Échec de la mise à jour de la mise à disposition.`
        });
      }
    });
  }
}
