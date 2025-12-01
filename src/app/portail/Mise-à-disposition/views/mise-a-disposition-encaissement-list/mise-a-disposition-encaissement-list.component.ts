import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MiseADispositionResponseDto} from "../../dtos/MiseADispositionResponseDto";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate, SelectItem} from "primeng/api";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {Subscription} from "rxjs";
import {MiseADispositionService} from "../../service/MiseADispositionService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {MiseADispositionSearchDto} from "../../dtos/MiseADispositionSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DatePipe, DecimalPipe, NgClass, NgIf} from "@angular/common";
import {DropdownModule} from "primeng/dropdown";
import {InputSwitchModule} from "primeng/inputswitch";
import {InputTextModule} from "primeng/inputtext";
import {RouterLinkActive} from "@angular/router";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {Ripple} from "primeng/ripple";

@Component({
  selector: 'app-mise-a-disposition-encaissement-list',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    FormsModule,
    InputSwitchModule,
    InputTextModule,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    NgClass,
    Ripple
  ],
  templateUrl: './mise-a-disposition-encaissement-list.component.html',
  styleUrl: './mise-a-disposition-encaissement-list.component.scss'
})
export class MiseADispositionEncaissementListComponent implements OnInit, OnDestroy {
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
      {icon: 'pi pi-list', route: '/portail/Mise-a-disposition/encaissement', label: "Les mises à disposition (encaissements)"},
    ];

    this.isAdmin = this.authService.hasRole(Role.ADMIN);
    this.initSearchForm();
    this.loadBanques();
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
      emetteur: [''],
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

    const searchCriteria: MiseADispositionSearchDto = this.searchForm.getRawValue() as MiseADispositionSearchDto;

    this.miseADispositionService.getEncaissementsForMiseADisposition(
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

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadMisesADisposition(this.dt.createLazyLoadMetadata());
    }
  }


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


  confirmToggleStatus(miseADisposition: MiseADispositionResponseDto): void {
    const confirmationMessage = `Êtes-vous sûr de vouloir ${miseADisposition.active ? 'désactiver' : 'activer'} la mise à disposition avec l'id ${miseADisposition.id}?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${miseADisposition.active ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: miseADisposition.active ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleMiseADispositionStatus(miseADisposition.id);
      }
    });
  }


  toggleMiseADispositionStatus(id: number): void {
    this.miseADispositionService.toggleMiseADispositionStatus(id).subscribe({
      next: (updatedMiseADisposition) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Mise à disposition avec id  ${updatedMiseADisposition.id} ${updatedMiseADisposition.active ? 'activée' : 'désactivée'} avec succès.`
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



  deleteMiseAdispo(miseADisposition: MiseADispositionResponseDto): void {
    const confirmationMessageHtml = `
    <div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right: 5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer la mise à disposition dont l'id est  "<strong>${miseADisposition.id}</strong>" ?</p>
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
        this.miseADispositionService.deleteMiseAdisposition(miseADisposition.id)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'La mise à disposition a été supprimée avec succès.', life: 3000 });
              this.loadMisesADisposition(this.lastLazyLoadEvent)
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors de la suppression de la mise à disposition :', error);
              const errorMessage = error.error.detail || 'Échec de la suppression de la mise à disposition .';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }
}
