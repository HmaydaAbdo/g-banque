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
import {VirementService} from "../../service/VirementService";
import {VirementResponseDto} from "../../dtos/VirementResponseDto";
import {VirementSearchDto} from "../../dtos/VirementSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {Type} from "../../Type";
import {MarcheResponseDTO} from "../../../Marches/dtos/marche-response.dto";
import {CalendarModule} from "primeng/calendar";
import {InputNumberModule} from "primeng/inputnumber";


@Component({
  selector: 'app-virement-decaissement-list',
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
    CalendarModule,
    InputNumberModule
  ],
  templateUrl: './virement-decaissement-list.component.html',
  styleUrl: './virement-decaissement-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class VirementDecaissementListComponent implements OnInit, OnDestroy {
  virements: VirementResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Tous les virements', value: null},
    {label: 'Les virements en cours', value: true},
    {label: 'Les virements archivés', value: false}
  ];
  typeOptions: SelectItem[] = [
    { label: 'Tous les types', value: null },
    { label: 'Encaissement', value: Type.ENCAISSEMENT },
    { label: 'Décaissement', value: Type.DECAISSEMENT }
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
    private virementService: VirementService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/Virements/decaissement', label: "Les Virements ( décaissement )" },
    ];
    this.isAdmin = this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
    this.loadVirements(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the search form with all filter controls.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      beneficiaire: [''],
      nomBanqueEmettrice: [null],
      active: [null],
      dateEmission: [null],
      montant: [null],
      banqueBeneficiaire: [null],
    });

    const searchSubscription = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(searchSubscription);
  }

  /**
   * Loads virements based on lazy load event and form filters.
   * @param event The PrimeNG lazy load event.
   */
  loadVirements(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: VirementSearchDto = this.searchForm.getRawValue();

    this.virementService.getDecaissementVirements(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<VirementResponseDto>) => {
        this.virements = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des virements:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des virements.'
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
      this.loadVirements(this.dt.createLazyLoadMetadata());
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
          this.banqueOptions.unshift({label: 'Toutes les banques émettrices', value: null});
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
      this.loadVirements(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Confirms and toggles the status of a virement.
   * @param virement The virement to toggle.
   */
  confirmToggleStatus(virement: VirementResponseDto): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${virement.active ? 'désactiver' : 'activer'} le virement  ${virement.id}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${virement.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: virement.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleVirementStatus(virement.id);
      }
    });
  }


  toggleVirementStatus(id: number): void {
    this.virementService.toggleVirementStatus(id).subscribe({
      next: (updatedVirement) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Virement  ${updatedVirement.id} ${updatedVirement.active ? 'activé' : 'désactivé'} avec succès.`
        });
        // Reload data from the server to reflect the change
        if (this.dt) {
          this.loadVirements(this.dt.createLazyLoadMetadata());
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du virement:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `Échec de la mise à jour du virement.`
        });
      }
    });
  }

  deleteVirement(virement: VirementResponseDto): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer ce virement "<strong>${virement.id}</strong>" ?</p>
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
        this.virementService.deleteVirement(virement.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Le marché a été supprimé avec succès.', life: 3000 });
              this.loadVirements(this.lastLazyLoadEvent)
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression du marché:', error);
              const errorMessage = error.error.detail || 'Échec de la suppression du marché.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

}
