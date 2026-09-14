import { Component, OnInit, ViewChild, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ContratLeasingService } from '../../services/contrat-leasing.service';
import { ConfirmationService, MenuItem, MessageService } from "primeng/api";
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
import { ContratLeasingRequestDTO } from "../../dtos/contrat-leasing";
import { DividerModule } from "primeng/divider";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ProgressSpinnerModule } from "primeng/progressspinner";
import { InputNumberModule } from "primeng/inputnumber";
import { parseLocalDate } from '../../../../global/types/date.utils';
import {AuthService} from "../../../../../auth/services/auth.service";
import { CalendarModule } from "primeng/calendar";

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
    BreadcrumbModule,
    ProgressSpinnerModule,
    InputNumberModule,
    CalendarModule
  ],
  providers: [MessageService, DialogService, ConfirmationService]
})
export class ContratLeasingListComponent implements OnInit, OnDestroy {

  private readonly contratLeasingService = inject(ContratLeasingService);
  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly banqueService = inject(BanqueService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);
  private readonly wizardService = inject(LeasingContractWizardService);
  private readonly authService = inject(AuthService);

  contrats: ContratLeasingResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;

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

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing', label: "Les Contrats Leasing Encours" },
    ];

    this.initSearchForm();
    this.loadDropdownOptions();

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
      montantEcheance: [null],
      dateDebutMin: [null as Date | null],
      dateDebutMax: [null as Date | null]
    });
  }

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    return (v.dateDebutMin || v.dateDebutMax) ? 1 : 0;
  }

  loadDropdownOptions(): void {
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeLeasingOptions = response.content.map(org => ({
            label: org.nom,
            value: org.id!
          }));
        }
      });

    this.banqueService.getAllBanques({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banqueOptions = response.content.map(banque => ({
            label: banque.nom,
            value: banque.id!
          }));
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
      montantEcheance: this.searchForm.get('montantEcheance')?.value || undefined,
      dateDebutMin: this.searchForm.get('dateDebutMin')?.value ?? undefined,
      dateDebutMax: this.searchForm.get('dateDebutMax')?.value ?? undefined
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
      error: () => {
        this.loading = false;
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
      montantEcheance: null,
      dateDebutMin: null,
      dateDebutMax: null
    });

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats(this.lastLazyLoadEvent);
    }
  }

  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: ContratLeasingSearchDTO = {
      numeroContrat: this.searchForm.get('numeroContrat')?.value || undefined,
      immobilisation: this.searchForm.get('immobilisation')?.value || undefined,
      typeContrat: this.searchForm.get('typeContrat')?.value || undefined,
      organismeLeasingId: this.searchForm.get('organismeLeasingId')?.value || undefined,
      banqueDePrelevementId: this.searchForm.get('banqueDePrelevementId')?.value || undefined,
      montantEcheance: this.searchForm.get('montantEcheance')?.value || undefined,
      dateDebutMin: this.searchForm.get('dateDebutMin')?.value ?? undefined,
      dateDebutMax: this.searchForm.get('dateDebutMax')?.value ?? undefined
    };

    this.contratLeasingService.exportActiveContratsPdf(searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrats_leasing_actifs_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      }
    });
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
      error: () => {
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
        const contratRequest: ContratLeasingRequestDTO = {
          id: contrat.id,
          numeroContrat: contrat.numeroContrat,
          immobilisation: contrat.immobilisation,
          montantAchatTTC: contrat.montantAchatTTC,
          dateDebut: parseLocalDate(contrat.dateDebut),
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
      error: () => {
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
      accept: () => this.deleteContrat(contrat.id)
    });
  }

  deleteContrat(id: number): void {
    this.contratLeasingService.deleteContratLeasing(id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Contrat supprimé avec succès.'
        });
        this.refreshCurrentContratInList();
      }
    });
  }

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

  private formatCurrency(amount: number | any): string {
    const numAmount = typeof amount === 'number' ? amount : Number(amount);
    return `${numAmount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} MAD`;
  }

  isSystemAdmin(): boolean {
    return this.authService.isSystemAdmin();
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
  }
}
