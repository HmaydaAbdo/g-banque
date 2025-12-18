import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { ContratLeasingService } from '../../services/contrat-leasing.service';
import {ConfirmationService, MenuItem, MessageService} from "primeng/api";
import { DialogService } from "primeng/dynamicdialog";
import { ContratLeasingResponseDTO } from "../../dtos/contrat-leasing";
import { ContratLeasingSearchDTO } from "../../dtos/contrat-leasing-search";
import { TypeContratLeasing } from "../../dtos/type-contrat-leasing";
import { TableLazyLoadEvent, Table } from "primeng/table";
import { PageResponse } from "../../../../global/types/PageResponse";
import { debounceTime, distinctUntilChanged, Subscription, take } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from "primeng/ripple";
import { OrganismeLeasingService } from "../../../organismesLeasing/services/organisme-leasing.service";
import { BanqueService } from "../../../../comptes-bancaire/services/BanqueService";
import { OrganismeLeasingResponse } from "../../../organismesLeasing/dtos/OrganismeLeasingResponse";
import { BanqueResponse } from "../../../../comptes-bancaire/dtos/BanqueResponse";
import { DialogModule } from 'primeng/dialog';

import { LeasingContractWizardService } from '../../services/LeasingContractWizardService';
import { ContratLeasingRequestDTO } from "../../dtos/contrat-leasing"; // Ensure this is imported

import { DividerModule } from "primeng/divider";
import { ContratLeasingDetailsComponent } from '../contrat-leasing-details/contrat-leasing-details.component';
import {Decimal} from "decimal.js";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';


@Component({
  selector: 'app-contrat-leasing-list',
  templateUrl: './contrats-leasing-list.component.html',
  styleUrls: ['./contrats-leasing-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    InputTextModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ConfirmDialogModule,
    RippleModule,
    DialogModule,
    DividerModule,
    ContratLeasingDetailsComponent,
    BreadcrumbModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService, DialogService, ConfirmationService]
})
export class ContratLeasingListComponent implements OnInit, OnDestroy {

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
    private sanitizer: DomSanitizer,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private wizardService: LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing', label:"Les Contrats Leasing Encours" },
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

    this.contratLeasingService.getActiveContrats(
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
          valeurResiduelle: contrat.valeurResiduelle ,
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
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger le PDF.' });
        this.loadingPdf = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0 && this.selectedContratForPdf) {
      const file = input.files[0];

      if (file.type !== 'application/pdf') {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Seuls les fichiers PDF sont acceptés.' });
        return;
      }

      this.loadingPdf = true;
      this.contratLeasingService.uploadPdfToContrat(this.selectedContratForPdf.id, file).subscribe({
        next: (message) => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: message });
          this.selectedContratForPdf!.hasFichierPdf = true;
          this.loadPdfPreview(this.selectedContratForPdf!.id);
          this.refreshCurrentContratInList();
        },
        error: (error) => {
          console.error('Erreur lors de l\'upload:', error);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec de l\'upload du PDF.' });
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
      accept: () => {
        this.deletePdf();
      }
    });
  }

  deletePdf(): void {
    if (this.selectedContratForPdf) {
      this.contratLeasingService.deletePdfFromContrat(this.selectedContratForPdf.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'PDF supprimé avec succès.' });
          this.selectedContratForPdf!.hasFichierPdf = false;
          this.pdfBlobUrl = null;
          this.refreshCurrentContratInList();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec de la suppression du PDF.' });
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
