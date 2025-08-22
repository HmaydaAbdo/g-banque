import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ContratLeasingRequestDTO, ContratLeasingResponseDTO} from "../../dtos/contrat-leasing";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TypeContratLeasing} from "../../dtos/type-contrat-leasing";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {debounceTime, distinctUntilChanged, Subscription, take} from "rxjs";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {ContratLeasingService} from "../../services/contrat-leasing.service";
import {OrganismeLeasingService} from "../../../organismesLeasing/services/organisme-leasing.service";
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {Router, RouterLinkActive} from "@angular/router";
import {LeasingContractWizardService} from "../../services/LeasingContractWizardService";
import {PageResponse} from "../../../../global/types/PageResponse";
import {OrganismeLeasingResponse} from "../../../organismesLeasing/dtos/OrganismeLeasingResponse";
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {ContratLeasingSearchDTO} from "../../dtos/contrat-leasing-search";
import {Decimal} from "decimal.js";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button, ButtonDirective} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ContratLeasingDetailsComponent} from "../contrat-leasing-details/contrat-leasing-details.component";
import {DatePipe, NgClass, NgIf} from "@angular/common";
import {DialogModule} from "primeng/dialog";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {Ripple} from "primeng/ripple";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'app-contrats-leasing-en-attente-de-ml',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ButtonDirective,
    ConfirmDialogModule,
    ContratLeasingDetailsComponent,
    DatePipe,
    DialogModule,
    DropdownModule,
    InputTextModule,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass
  ],
  templateUrl: './contrats-leasing-en-attente-de-ml.component.html',
  styleUrl: './contrats-leasing-en-attente-de-ml.component.scss'
})
export class ContratsLeasingEnAttenteDeMLComponent implements OnInit, OnDestroy {

  contrats: ContratLeasingResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;

  searchForm!: FormGroup;

  typeContratOptions = Object.values(TypeContratLeasing).map(type => ({ label: type, value: type }));
  organismeLeasingOptions: { label: string, value: number }[] = [];
  banqueOptions: { label: string, value: number }[] = [];



  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: 1,
    filters: {}
  };

  displayDetailsDialog: boolean = false;
  selectedContratDetails: ContratLeasingResponseDTO | null = null;
  items: MenuItem[] | undefined;


  constructor(
    private contratLeasingService: ContratLeasingService,
    private organismeLeasingService: OrganismeLeasingService,
    private banqueService: BanqueService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private wizardService: LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing/contratsLeasing/en-attente-de-ML', label:"Les Contracts Leasing En Attente de ML" },
    ]

    this.initSearchForm();
    this.loadDropdownOptions();

    const numeroContratSub = this.searchForm.get('numeroContrat')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const immobilisationSub = this.searchForm.get('immobilisation')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const typeContratSub = this.searchForm.get('typeContrat')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const organismeLeasingSub = this.searchForm.get('organismeLeasingId')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const banqueDePrelevementSub = this.searchForm.get('banqueDePrelevementId')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());




    this.subscriptions.push(numeroContratSub!, immobilisationSub!, typeContratSub!, organismeLeasingSub!, banqueDePrelevementSub!);

    this.loadContrats(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroContrat: [''],
      immobilisation: [''],
      typeContrat: [null],
      organismeLeasingId: [null],
      banqueDePrelevementId: [null],
      completed: [false]
    });
  }

  loadDropdownOptions(): void {
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeLeasingOptions = response.content.map(org => ({
            label: org.nom,
            value: org.id!
          }));
        },
        error: (error) => {
          console.error('Error loading organisme leasing options:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load leasing organisms.' });
        }
      });

    this.banqueService.getAllBanques({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banqueOptions = response.content.map(banque => ({
            label: banque.nom,
            value: banque.id!
          }));
        },
        error: (error) => {
          console.error('Error loading bank options:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load banks.' });
        }
      });
  }

  loadContrats(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'id,asc';
    if (event.sortField && event.sortOrder) {
      const actualSortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sort = `${actualSortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: ContratLeasingSearchDTO = {
      numeroContrat: this.searchForm.get('numeroContrat')?.value,
      immobilisation: this.searchForm.get('immobilisation')?.value,
      typeContrat: this.searchForm.get('typeContrat')?.value,
      organismeLeasingId: this.searchForm.get('organismeLeasingId')?.value,
      banqueDePrelevementId: this.searchForm.get('banqueDePrelevementId')?.value
    };

    this.contratLeasingService.getPendingLienReleaseContrats(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<ContratLeasingResponseDTO>) => {
        this.contrats = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des contrats:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des contrats.' });
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats({
        ...this.lastLazyLoadEvent,
        first: 0,
        filters: {}
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      numeroContrat: '',
      immobilisation: '',
      typeContrat: null,
      organismeLeasingId: null,
      banqueDePrelevementId: null,
      completed: null // ⭐ Réinitialisation du filtre
    });

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats(this.lastLazyLoadEvent);
    }
  }

  createNewContrat(): void {
    this.wizardService.resetContract();
    this.router.navigate(['portail/leasing/contratsLeasing/creation/organisme']);
  }

  onRowSelect(event: any): void {
    const contratId = event.data.id;
    this.getContratDetailsForDialog(contratId);
  }

  getContratDetailsForDialog(id: number): void {
    this.loading = true;
    this.contratLeasingService.getContratLeasingById(id).subscribe({
      next: (contratDetails: ContratLeasingResponseDTO) => {
        this.selectedContratDetails = contratDetails;
        this.displayDetailsDialog = true;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error fetching contract details:', error);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load contract details.' });
        this.loading = false;
      }
    });
  }

  hideDetailsDialog(): void {
    this.displayDetailsDialog = false;
    this.selectedContratDetails = null;
  }

  viewAmortizationTable(contratId: number): void {
    this.router.navigate(['portail/leasing/contratsLeasing', contratId, 'tableau-amortissement']);
  }
  editContrat(id: number): void {
    this.loading = true;
    this.contratLeasingService.getContratLeasingById(id).pipe(
      take(1)
    ).subscribe({
      next: (contrat: ContratLeasingResponseDTO) => {
        // Ensure that 'contrat.phases' is treated as an array, even if it could be null/undefined
        // And ensure Decimal types are correctly assigned to Decimal | null if that's the DTO type.
        // Your ContratLeasingService's processContratLeasingResponse should already ensure
        // montantAchatTTC, valeurResiduelle, and mensualiteTTC are Decimal or null.
        const contratRequest: ContratLeasingRequestDTO = {
          id: contrat.id,
          numeroContrat: contrat.numeroContrat,
          immobilisation: contrat.immobilisation,
          // These should already be Decimal | null from ContratLeasingService
          montantAchatTTC: contrat.montantAchatTTC,
          dateDebut: contrat.dateDebut,
          periodeEnMois: contrat.periodeEnMois,
          organismeLeasingId: contrat.organismeLeasingId,
          banqueDePrelevementId: contrat.banqueDePrelevementId,
          typeContrat: contrat.typeContrat,
          valeurResiduelle: contrat.valeurResiduelle as Decimal,
          // Use a nullish coalescing operator to ensure phases is always an array
          phases: (contrat.phases ?? []).map(p => ({
            id: p.id,
            numeroPhase: p.numeroPhase,
            dureeMois: p.dureeMois,
            // mensualiteTTC should also be Decimal | null
            mensualiteTTC: p.mensualiteTTC,
          }))
        };

        this.wizardService.loadContractForEdit(contratRequest);
        this.router.navigate(['portail/leasing/contratsLeasing/creation/organisme']);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error fetching contract for edit:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement du contrat pour modification.' });
        this.loading = false;
      }
    });
  }

  confirmDeleteContrat(contrat: ContratLeasingResponseDTO): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le contrat N° ${contrat.numeroContrat} ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.deleteContrat(contrat.id);
      }
    });
  }

  deleteContrat(id: number): void {
    this.contratLeasingService.deleteContratLeasing(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat supprimé avec succès.' });
        if (this.dt) {
          this.loadContrats(this.dt.createLazyLoadMetadata());
        } else {
          this.loadContrats(this.lastLazyLoadEvent);
        }
      },
      error: (error) => {
        console.error('Erreur lors de la suppression:', error);
        let errorMessage = 'Échec de la suppression du contrat.';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage });
      }
    });
  }

  /**
   * Confirms the archiving of a leasing contract with a dialog.
   * @param contrat The contract to be archived.
   */
  confirmArchiveContrat(contrat: ContratLeasingResponseDTO): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir archiver le contrat N° ${contrat.numeroContrat} ?`,
      header: 'Confirmation d\'archivage',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.archiveContrat(contrat.id);
      }
    });
  }

  /**
   * Archives a contract by calling the service.
   * @param id The ID of the contract to archive.
   */
  archiveContrat(id: number | undefined): void {
    if (id === undefined) {
      this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'ID du contrat non valide.' });
      return;
    }
    this.contratLeasingService.markAsInactive(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contrat archivé avec succès.' });
        // Refresh the table data after a successful archive
        if (this.dt) {
          this.loadContrats(this.dt.createLazyLoadMetadata());
        } else {
          this.loadContrats(this.lastLazyLoadEvent);
        }
      },
      error: (error) => {
        console.error('Erreur lors de l\'archivage:', error);
        let errorMessage = 'Échec de l\'archivage du contrat.';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage });
      }
    });
  }
}
