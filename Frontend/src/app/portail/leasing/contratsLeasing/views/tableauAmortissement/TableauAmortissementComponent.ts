import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { AVERTISSEMENT_RETOUR_EN_COURS } from '../../../../global/utils/bascule-statut.utils';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

// PrimeNG Modules
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputSwitchModule } from 'primeng/inputswitch';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { RippleModule } from 'primeng/ripple';

import { TableauAmortissement } from '../../dtos/table-amortissement';
import { EcheanceLeasingResponse } from '../../dtos/EcheanceLeasingResponse';
import { StatutEcheanceLeasing } from '../../dtos/StatutEcheanceLeasing';
import { EcheanceLeasingService } from '../../services/EcheanceLeasingService';
import { EcheanceLeasingSearchDTO } from '../../dtos/echeance-leasing-search';
import { AuthService } from '../../../../../auth/services/auth.service';
import { CalendarModule } from 'primeng/calendar';
import { ContratLeasingService } from '../../services/contrat-leasing.service';
import { AvanceLeasingResponse, LIBELLE_TYPE_MOYEN_PAIEMENT } from '../../dtos/avance-leasing';


@Component({
  selector: 'app-tableau-amortissement',
  templateUrl: './tableau-amortissement.component.html',
  styleUrls: ['./tableau-amortissement.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ProgressSpinnerModule,
    DividerModule,
    InputNumberModule,
    InputSwitchModule,
    FormsModule,
    BreadcrumbModule,
    CalendarModule,
    RippleModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class TableauAmortissementComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly echeanceLeasingService = inject(EcheanceLeasingService);
  private readonly contratLeasingService = inject(ContratLeasingService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  contratId!: number;
  tableauAmortissement: TableauAmortissement | null = null;
  echeances: EcheanceLeasingResponse[] = [];
  totalEcheancesRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  errorMessage: string | null = null;

  /** Avance du contrat (échéance n°1) et son moyen de paiement ; null si le contrat n'a pas d'avance. */
  avance: AvanceLeasingResponse | null = null;
  readonly libelleMoyen = LIBELLE_TYPE_MOYEN_PAIEMENT;

  searchForm!: FormGroup;
  statutLeasingOptions = Object.values(StatutEcheanceLeasing).map(status => ({ label: status, value: status }));

  private subscriptions: Subscription[] = [];

  protected lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 3,
    sortField: 'id',
    sortOrder: 1,
    filters: {}
  };
  items: MenuItem[] | undefined;

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/leasing', label: 'Les Contracts Leasing' },
      { icon: 'pi pi-table', route: '/portail/leasing', label: "Tableau d'amortissement" },
    ];
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.contratId = +id;
        this.initSearchForm();
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
        this.chargerAvance();
      } else {
        this.errorMessage = 'Contract ID not provided in the URL.';
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroEcheance: [null],
      dateEcheanceDebut: [null as Date | null],
      dateEcheanceFin: [null as Date | null],
      dateOperationDebut: [null as Date | null],
      dateOperationFin: [null as Date | null],
      dateValeurDebut: [null as Date | null],
      dateValeurFin: [null as Date | null],
      statutPaiement: [null]
    });

    const searchSub = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.lastLazyLoadEvent.first = 0;
      this.loadTableauAmortissement(this.lastLazyLoadEvent);
    });
    this.subscriptions.push(searchSub);
  }

  /** Charge l'avance seulement si le contrat en a une (évite une requête en erreur pour les autres contrats). */
  private chargerAvance(): void {
    this.contratLeasingService.getContratLeasingById(this.contratId).pipe(
      switchMap(contrat => contrat.avanceTTC ? this.echeanceLeasingService.getAvance(this.contratId) : EMPTY)
    ).subscribe(avance => this.avance = avance);
  }

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEcheanceDebut || v.dateEcheanceFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
  }

  loadTableauAmortissement(event: TableLazyLoadEvent): void {
    if (!this.contratId) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sortBy: string | undefined;
    let sortDirection: 'asc' | 'desc' | undefined;

    if (event.sortField) {
      sortBy = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    } else {
      sortBy = this.lastLazyLoadEvent.sortField as string;
      sortDirection = this.lastLazyLoadEvent.sortOrder === 1 ? 'asc' : 'desc';
    }

    const searchCriteria: EcheanceLeasingSearchDTO = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
      statutPaiement: this.searchForm.get('statutPaiement')?.value
    };

    this.echeanceLeasingService.getTableauAmortissementForContrat(
      this.contratId,
      searchCriteria,
      currentPage,
      pageSize,
      sortBy,
      sortDirection
    ).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (data: TableauAmortissement) => {
        this.tableauAmortissement = data;
        this.echeances = data.echeances.content;
        this.totalEcheancesRecords = data.echeances.page.totalElements;
      },
      error: () => {
        this.errorMessage = 'Échec du chargement du tableau d\'amortissement. Veuillez réessayer.';
      }
    });
  }

  exportPdf(): void {
    if (!this.contratId) return;
    this.exporting = true;

    const searchCriteria: EcheanceLeasingSearchDTO = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
      statutPaiement: this.searchForm.get('statutPaiement')?.value
    };

    this.echeanceLeasingService.exportTableauAmortissementPdf(this.contratId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `tableau_amortissement_leasing_${this.contratId}_${new Date().toISOString().slice(0, 10)}.pdf`;
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

  clearSearch(): void {
    this.searchForm.reset({
      numeroEcheance: null,
      dateEcheanceDebut: null,
      dateEcheanceFin: null,
      dateOperationDebut: null,
      dateOperationFin: null,
      dateValeurDebut: null,
      dateValeurFin: null,
      statutPaiement: null
    });
  }

  getStatutSeverity(status: StatutEcheanceLeasing): 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' | undefined {
    switch (status) {
      case StatutEcheanceLeasing.PAYE:
        return 'success';
      case StatutEcheanceLeasing.PREVU:
        return 'info';
      default:
        return 'warning';
    }
  }

  /** Le retour « prévu » d'une échéance payée peut supprimer sa transaction de rapprochement : confirmation explicite. */
  onStatutChange(echeance: EcheanceLeasingResponse): void {
    if (echeance.statutPaiement !== StatutEcheanceLeasing.PAYE) {
      this.basculerStatut(echeance);
      return;
    }
    this.confirmationService.confirm({
      message: 'Voulez-vous remettre cette échéance à « prévu » ? ' + AVERTISSEMENT_RETOUR_EN_COURS,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => this.basculerStatut(echeance),
      reject: () => this.loadTableauAmortissement(this.lastLazyLoadEvent)
    });
  }

  private basculerStatut(echeance: EcheanceLeasingResponse): void {
    this.loading = true;
    this.echeanceLeasingService.toggleStatus(echeance.id).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: () => {
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
      },
      error: () => {
        this.errorMessage = 'Échec de la mise à jour du statut. Veuillez réessayer.';
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  /** Navigue vers la page d'édition dédiée de cette échéance (remplace l'ancien dialogue). */
  editEcheance(echeance: EcheanceLeasingResponse): void {
    this.router.navigate(['/portail/leasing/echeances/edit', echeance.id]);
  }
}
