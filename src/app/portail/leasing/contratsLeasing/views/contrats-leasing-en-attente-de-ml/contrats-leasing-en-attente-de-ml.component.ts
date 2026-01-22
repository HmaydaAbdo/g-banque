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
import {PageResponse} from "../../../../global/types/PageResponse";
import {OrganismeLeasingResponse} from "../../../organismesLeasing/dtos/OrganismeLeasingResponse";
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {ContratLeasingSearchDTO} from "../../dtos/contrat-leasing-search";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ContratLeasingDetailsComponent} from "../contrat-leasing-details/contrat-leasing-details.component";
import {DatePipe, DecimalPipe, NgClass, NgIf} from "@angular/common";
import {DialogModule} from "primeng/dialog";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {InputNumberModule} from "primeng/inputnumber";
import {LeasingContractWizardService} from "../../services/LeasingContractWizardService";

@Component({
  selector: 'app-contrats-leasing-en-attente-de-ml',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ConfirmDialogModule,
    ContratLeasingDetailsComponent,
    DatePipe,
    DialogModule,
    DropdownModule,
    InputTextModule,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass,
    DecimalPipe,
    ProgressSpinnerModule,
    InputNumberModule
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

  // Gestion du dialog PDF
  displayPdfDialog: boolean = false;
  selectedContratForPdf: ContratLeasingResponseDTO | null = null;
  pdfBlobUrl: SafeResourceUrl | null = null;
  pdfFileName: string = '';
  pdfFileSize: string = '';
  loadingPdf: boolean = false;

  constructor(
    private contratLeasingService: ContratLeasingService,
    private organismeLeasingService: OrganismeLeasingService,
    private banqueService: BanqueService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private sanitizer: DomSanitizer,
    private wizardService:LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing/contratsLeasing/en-attente-de-ML', label: "Les Contrats Leasing En Attente de ML" },
    ];

    this.initSearchForm();
    this.loadDropdownOptions();

    // ⭐ OPTIMISATION : Une seule subscription pour tout le formulaire
    const searchFormSub = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => this.onSearch());

    this.subscriptions.push(searchFormSub);

    this.loadContrats(this.lastLazyLoadEvent);
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroContrat: [''],
      immobilisation: [''],
      typeContrat: [null],
      organismeLeasingId: [null],
      banqueDePrelevementId: [null],
      completed: [false],
      montantEcheance: [null]
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
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des organismes de leasing.'
          });
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
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des banques.'
          });
        }
      });
  }

  loadContrats(event: TableLazyLoadEvent): void {
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
      numeroContrat: this.searchForm.get('numeroContrat')?.value || undefined,
      immobilisation: this.searchForm.get('immobilisation')?.value || undefined,
      typeContrat: this.searchForm.get('typeContrat')?.value || undefined,
      organismeLeasingId: this.searchForm.get('organismeLeasingId')?.value || undefined,
      banqueDePrelevementId: this.searchForm.get('banqueDePrelevementId')?.value || undefined,
      montantEcheance: this.searchForm.get('montantEcheance')?.value || undefined
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
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des contrats.'
        });
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
      completed: null,
      montantEcheance: null
    });

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats(this.lastLazyLoadEvent);
    }
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
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des détails du contrat.'
        });
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

  /**
   * Confirms the archiving of a leasing contract with a dialog.
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
      accept: () => this.archiveContrat(contrat.id)
    });
  }

  editContrat(id: number): void {
    this.loading = true;
    this.contratLeasingService.getContratLeasingById(id).pipe(
      take(1)
    ).subscribe({
      next: (contrat: ContratLeasingResponseDTO) => {
        const contratRequest: ContratLeasingRequestDTO = {
          id: contrat.id,
          numeroContrat: contrat.numeroContrat,
          immobilisation: contrat.immobilisation,
          montantAchatTTC: contrat.montantAchatTTC,
          dateDebut: contrat.dateDebut,
          periodeEnMois: contrat.periodeEnMois,
          organismeLeasingId: contrat.organismeLeasingId,
          banqueDePrelevementId: contrat.banqueDePrelevementId,
          typeContrat: contrat.typeContrat,
          valeurResiduelle: contrat.valeurResiduelle,
          phases: (contrat.phases ?? []).map(p => ({
            id: p.id,
            numeroPhase: p.numeroPhase,
            dureeMois: p.dureeMois,
            mensualiteTTC: p.mensualiteTTC,
          }))
        };

        this.wizardService.loadContractForEdit(contratRequest);
        this.router.navigate(['portail/leasing/contratsLeasing/creation/organisme']);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error fetching contract for edit:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement du contrat pour modification.'
        });
        this.loading = false;
      }
    });
  }
  /**
   * Archives a contract by calling the service.
   */
  archiveContrat(id: number | undefined): void {
    if (id === undefined) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'ID du contrat non valide.'
      });
      return;
    }

    this.contratLeasingService.markAsInactive(id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Contrat archivé avec succès.'
        });
        this.refreshCurrentContratInList();
      },
      error: (error) => {
        console.error('Erreur lors de l\'archivage:', error);
        const errorMessage = error.error?.message || 'Échec de l\'archivage du contrat.';
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: errorMessage
        });
      }
    });
  }

  openPdfDialog(contrat: ContratLeasingResponseDTO): void {
    this.selectedContratForPdf = contrat;
    this.displayPdfDialog = true;

    if (contrat.hasFichierPdf) {
      this.loadPdfPreview(contrat.id);
    }
  }

  hidePdfDialog(): void {
    this.displayPdfDialog = false;
    this.selectedContratForPdf = null;
    this.pdfBlobUrl = null;
    this.pdfFileName = '';
    this.pdfFileSize = '';
  }

  loadPdfPreview(contratId: number): void {
    this.loadingPdf = true;

    this.contratLeasingService.downloadPdfFromContrat(contratId).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        this.pdfBlobUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.pdfFileName = `Contrat_${contratId}.pdf`;
        this.pdfFileSize = this.formatFileSize(blob.size);
        this.loadingPdf = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du PDF:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger le PDF.'
        });
        this.loadingPdf = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0 && this.selectedContratForPdf) {
      const file = input.files[0];

      if (file.type !== 'application/pdf') {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Seuls les fichiers PDF sont acceptés.'
        });
        return;
      }

      this.loadingPdf = true;
      this.contratLeasingService.uploadPdfToContrat(this.selectedContratForPdf.id, file).subscribe({
        next: (message) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: message
          });
          this.selectedContratForPdf!.hasFichierPdf = true;
          this.loadPdfPreview(this.selectedContratForPdf!.id);
          this.refreshCurrentContratInList();
        },
        error: (error) => {
          console.error('Erreur lors de l\'upload:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec de l\'upload du PDF.'
          });
          this.loadingPdf = false;
        }
      });
    }
  }

  downloadPdf(): void {
    if (this.selectedContratForPdf) {
      this.contratLeasingService.downloadPdfFile(
        this.selectedContratForPdf.id,
        `Contrat_${this.selectedContratForPdf.numeroContrat}.pdf`
      );
    }
  }

  confirmDeletePdf(): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer ce fichier PDF ?',
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.deletePdf()
    });
  }

  deletePdf(): void {
    if (this.selectedContratForPdf) {
      this.contratLeasingService.deletePdfFromContrat(this.selectedContratForPdf.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'PDF supprimé avec succès.'
          });
          this.selectedContratForPdf!.hasFichierPdf = false;
          this.pdfBlobUrl = null;
          this.refreshCurrentContratInList();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec de la suppression du PDF.'
          });
        }
      });
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Retourne l'affichage des échéances pour le tableau
   */
  getEcheancesDisplay(contrat: ContratLeasingResponseDTO): string {
    if (!contrat.phases || contrat.phases.length === 0) {
      return 'N/A';
    }

    if (contrat.phases.length === 1) {
      return this.formatCurrency(contrat.phases[0].mensualiteTTC);
    }

    const montants = contrat.phases.map(p => Number(p.mensualiteTTC));
    const min = Math.min(...montants);
    const max = Math.max(...montants);

    if (min === max) {
      return this.formatCurrency(min);
    }

    return `${this.formatCurrency(min)} - ${this.formatCurrency(max)}`;
  }

  /**
   * Retourne le contenu du tooltip avec toutes les échéances détaillées
   */
  getEcheancesTooltip(contrat: ContratLeasingResponseDTO): string {
    if (!contrat.phases || contrat.phases.length === 0) {
      return 'Aucune phase définie';
    }

    if (contrat.phases.length === 1) {
      return `Mensualité unique: ${this.formatCurrency(contrat.phases[0].mensualiteTTC)}`;
    }

    const sortedPhases = [...contrat.phases].sort((a, b) => a.numeroPhase - b.numeroPhase);

    let tooltip = 'Détail des échéances:\n';
    sortedPhases.forEach(phase => {
      tooltip += `\n• Phase ${phase.numeroPhase} (${phase.dureeMois} mois): ${this.formatCurrency(phase.mensualiteTTC)}`;
    });

    return tooltip;
  }

  /**
   * Formate un nombre en devise MAD
   */
  private formatCurrency(amount: number | any): string {
    const numAmount = typeof amount === 'number' ? amount : Number(amount);
    return `${numAmount.toFixed(2)} MAD`;
  }

  refreshCurrentContratInList(): void {
    if (this.dt) {
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats(this.lastLazyLoadEvent);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());

    if (this.pdfBlobUrl) {
      const url = (this.pdfBlobUrl as any).changingThisBreaksApplicationSecurity;
      if (url) {
        URL.revokeObjectURL(url);
      }
    }
  }
}
