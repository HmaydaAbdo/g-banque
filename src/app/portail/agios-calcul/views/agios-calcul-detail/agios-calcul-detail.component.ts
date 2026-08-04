import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

// PrimeNG
import { TableModule, Table, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService, MenuItem } from 'primeng/api';

// App
import { AgiosCalculService } from '../../service/agios-calcul.service';
import { AgiosTrimestrielSearchDto } from '../../dtos/AgiosTrimestrielSearchDto';
import { AgiosJournalierSearchDto } from '../../dtos/AgiosJournalierSearchDto';
import { AgiosTrimestrielResponseDto } from '../../dtos/AgiosTrimestrielResponseDto';
import { AgiosJournalierResponseDto } from '../../dtos/AgiosJournalierResponseDto';
import { MarcheRefDto } from '../../dtos/MarcheRefDto';
import { TypeAgios, TypeAgiosLabels } from '../../enums/TypeAgios';
import { StatutTrimestre } from '../../enums/StatutTrimestre';
import { AuthService } from '../../../../auth/services/auth.service';
import { AgiosDetailAggregateDto } from '../../dtos/Agiosdetailaggregatedto';

@Component({
  selector: 'app-agios-calcul-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe,
    TableModule, ButtonModule, RippleModule, BreadcrumbModule,
    ToastModule, TooltipModule, TagModule, CalendarModule, DropdownModule, ConfirmDialogModule
  ],
  templateUrl: './agios-calcul-detail.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosCalculDetailComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agiosCalculService = inject(AgiosCalculService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly authService = inject(AuthService);

  private destroy$ = new Subject<void>();

  // Route params
  banqueId!: number;
  typeAgios!: TypeAgios;
  trimestre!: number;
  annee!: number;

  // Data
  jours: AgiosJournalierResponseDto[] = [];
  totalRecords = 0;
  trimestreInfo?: AgiosTrimestrielResponseDto;
  aggregates?: AgiosDetailAggregateDto;
  loading = false;
  exporting = false;

  // Pagination
  rows = 31;
  first = 0;

  // Date limite filter (Reactive Forms)
  dateLimiteControl = new FormControl<Date | null>(null);
  minDate!: Date;
  maxDate!: Date;

  // Marché filter (Reactive Forms) — pertinent uniquement pour les types multi-marchés
  marcheIdControl = new FormControl<number | null>(null);
  marcheOptions: MarcheRefDto[] = [];

  // Search DTO (réutilisé pour table + agrégats)
  private journalierDto!: AgiosJournalierSearchDto;

  // Computed
  banqueNom = '';
  typeLabel = '';

  // Breadcrumb
  items: MenuItem[] = [];

  // Template refs
  readonly StatutTrimestre = StatutTrimestre;

  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.banqueId = +params['banqueId'];
        this.typeAgios = params['type'] as TypeAgios;
        this.trimestre = +params['trim'];
        this.annee = +params['annee'];
        this.typeLabel = TypeAgiosLabels[this.typeAgios] || this.typeAgios;

        // Bornes de la période pour le calendrier (mois ou trimestre selon le type)
        if (this.typeAgios === TypeAgios.FACTORING) {
          // FACTORING : période mensuelle → trimestre contient le numéro de mois
          this.minDate = new Date(this.annee, this.trimestre - 1, 1);
          this.maxDate = new Date(this.annee, this.trimestre, 0); // dernier jour du mois
        } else {
          const moisDebut = (this.trimestre - 1) * 3;
          this.minDate = new Date(this.annee, moisDebut, 1);
          this.maxDate = new Date(this.annee, moisDebut + 3, 0); // dernier jour du trimestre
        }

        // DTO de base
        this.journalierDto = {
          banqueId: this.banqueId,
          type: this.typeAgios,
          trimestre: this.trimestre,
          annee: this.annee
        };

        this.items = [
          { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
          { icon: 'pi pi-calculator', route: '/portail/agios/calcul', label: 'Calcul des Agios' },
          { label: this.periodeLabel }
        ];

        this.loadTrimestreInfo();
        this.loadAggregates();
        if (this.isMultiMarche) {
          this.loadMarchesDisponibles();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ═══════════════════════════════════════════════════════════════
  //  CHARGEMENT — SERVER-SIDE
  // ═══════════════════════════════════════════════════════════════

  loadTrimestreInfo(): void {
    const dto: AgiosTrimestrielSearchDto = {
      banqueId: this.banqueId,
      type: this.typeAgios,
      annee: this.annee,
      trimestre: this.trimestre
    };

    this.agiosCalculService.searchTrimestres(dto, 0, 1, 'annee,desc')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          if (res.content.length > 0) {
            this.trimestreInfo = res.content[0];
            this.banqueNom = this.trimestreInfo.banqueNom;
          }
        }
      });
  }

  loadAggregates(): void {
    this.agiosCalculService.getDetailAggregates(this.journalierDto)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: agg => this.aggregates = agg
      });
  }

  loadMarchesDisponibles(): void {
    this.agiosCalculService.getMarchesDisponibles(this.journalierDto)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: marches => this.marcheOptions = marches,
        error: () => {}
      });
  }

  loadJours(event: TableLazyLoadEvent): void {
    this.loading = true;

    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    const size = event.rows ?? this.rows;

    let sort = 'dateValeur,asc';
    if (event.sortField) {
      const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      const order = event.sortOrder === 1 ? 'asc' : 'desc';
      sort = `${field},${order}`;
    }

    this.agiosCalculService.searchDetail(this.journalierDto, page, size, sort)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: res => {
          this.jours = res.content;
          this.totalRecords = res.page.totalElements;
          if (res.content.length > 0 && !this.banqueNom) {
            this.banqueNom = res.content[0].banqueNom;
          }
        },
        error: () => {}
      });
  }

  // ═══════════════════════════════════════════════════════════════
  //  DATE LIMITE — FILTRE
  // ═══════════════════════════════════════════════════════════════

  onDateLimiteChange(dt: Table): void {
    const d = this.dateLimiteControl.value;
    if (d) {
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      this.journalierDto.dateLimite = `${yyyy}-${mm}-${dd}`;
    } else {
      this.journalierDto.dateLimite = undefined;
    }

    // Recharger table + agrégats
    this.first = 0;
    dt.first = 0;
    dt.onLazyLoad.emit({
      first: 0,
      rows: this.rows,
      sortField: 'id',
      sortOrder: 1
    });
    this.loadAggregates();
  }

  clearDateLimite(dt: Table): void {
    this.dateLimiteControl.setValue(null);
    this.onDateLimiteChange(dt);
  }

  // ═══════════════════════════════════════════════════════════════
  //  MARCHÉ — FILTRE
  // ═══════════════════════════════════════════════════════════════

  onMarcheChange(dt: Table): void {
    this.journalierDto.marcheId = this.marcheIdControl.value ?? undefined;

    // Recharger table + agrégats
    this.first = 0;
    dt.first = 0;
    dt.onLazyLoad.emit({
      first: 0,
      rows: this.rows,
      sortField: 'id',
      sortOrder: 1
    });
    this.loadAggregates();
  }

  clearMarche(dt: Table): void {
    this.marcheIdControl.setValue(null);
    this.onMarcheChange(dt);
  }

  // ═══════════════════════════════════════════════════════════════
  //  ACTIONS
  // ═══════════════════════════════════════════════════════════════

  cloturer(): void {
    if (!this.trimestreInfo) return;

    this.confirmationService.confirm({
      message: `Clôturer définitivement ${this.typeLabel} ${this.periodeLabel} pour ${this.banqueNom} ?
                Le montant TTC de ${this.trimestreInfo.totalAgiosTTC.toFixed(2)} MAD sera figé.`,
      header: 'Confirmer la clôture',
      icon: 'pi pi-lock',
      acceptLabel: 'Clôturer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.loading = true;
        this.agiosCalculService.cloturerTrimestre(
          this.banqueId, this.typeAgios, this.trimestre, this.annee
        ).pipe(
          takeUntil(this.destroy$),
          finalize(() => this.loading = false)
        ).subscribe({
          next: result => {
            this.messageService.add({
              severity: 'success', summary: 'Clôturé',
              detail: `${this.periodeLabel} clôturé — TTC: ${result.totalAgiosTTC.toFixed(2)} MAD`
            });
            this.loadTrimestreInfo();
            this.loadAggregates();
          },
          error: () => {}
        });
      }
    });
  }

  retour(): void {
    this.router.navigate(['/portail/agios/calcul']);
  }

  exportPdf(): void {
    this.exporting = true;

    this.agiosCalculService.exportJournalierPdf(this.journalierDto)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.exporting = false)
      )
      .subscribe({
        next: (blob: Blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `agios_calcul_detail_${new Date().toISOString().slice(0, 10)}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        }
      });
  }

  // ═══════════════════════════════════════════════════════════════
  //  HELPERS
  // ═══════════════════════════════════════════════════════════════

  getStatutSeverity(statut: StatutTrimestre): 'success' | 'danger' | 'warning' | 'info' {
    return statut === StatutTrimestre.CLOTURE ? 'success' : 'warning';
  }

  getStatutLabel(statut: StatutTrimestre): string {
    return statut === StatutTrimestre.CLOTURE ? 'Clôturé' : 'En cours';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  get hasDateFilter(): boolean {
    return !!this.journalierDto.dateLimite;
  }

  get hasMarcheFilter(): boolean {
    return this.journalierDto.marcheId != null;
  }

  /**
   * Libellé de la période affiché dans le header et le fil d'Ariane.
   * – FACTORING (mensuel) : "Juillet 2026"
   * – Autres (trimestriel) : "T2/2026"
   */
  get periodeLabel(): string {
    if (this.typeAgios === TypeAgios.FACTORING) {
      const mois = ['Janvier','Février','Mars','Avril','Mai','Juin',
                    'Juillet','Août','Septembre','Octobre','Novembre','Décembre'];
      return `${mois[(this.trimestre - 1) % 12]} ${this.annee}`;
    }
    return `T${this.trimestre}/${this.annee}`;
  }

  /**
   * Vrai pour les types multi-marchés (Préfinancement, Droits Constatés, Factoring) :
   * affiche la colonne Marché et adapte les libellés encours/jours.
   */
  get isMultiMarche(): boolean {
    return this.typeAgios === TypeAgios.PREFINANCEMENT
        || this.typeAgios === TypeAgios.DROIT_CONSTATE
        || this.typeAgios === TypeAgios.FACTORING;
  }

  /** Libellé de la colonne "Solde" : encours pour les types multi-marchés, solde comptable pour FC. */
  get soldeColLabel(): string {
    return this.isMultiMarche ? 'Encours' : 'Solde';
  }

  /** Libellé du KPI "jours actifs" : encours pour multi-marchés, découvert pour FC. */
  get joursActifsLabel(): string {
    return this.isMultiMarche ? 'JOURS ENCOURS' : 'JOURS DÉC.';
  }
}
