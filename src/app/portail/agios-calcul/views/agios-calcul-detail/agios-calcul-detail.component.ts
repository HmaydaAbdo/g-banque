import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, finalize, forkJoin } from 'rxjs';

// PrimeNG
import { TableModule, Table, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { CalendarModule } from 'primeng/calendar';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService, MenuItem } from 'primeng/api';

// App
import { AgiosCalculService } from '../../service/agios-calcul.service';
import { AgiosTrimestrielSearchDto } from '../../dtos/AgiosTrimestrielSearchDto';
import { AgiosJournalierSearchDto } from '../../dtos/AgiosJournalierSearchDto';
import { AgiosTrimestrielResponseDto } from '../../dtos/AgiosTrimestrielResponseDto';
import { AgiosJournalierResponseDto } from '../../dtos/AgiosJournalierResponseDto';
import { TypeAgios, TypeAgiosLabels } from '../../enums/TypeAgios';
import { StatutTrimestre } from '../../enums/StatutTrimestre';
import { AuthService } from '../../../../auth/services/auth.service';
import {AgiosDetailAggregateDto} from "../../dtos/Agiosdetailaggregatedto";

@Component({
  selector: 'app-agios-calcul-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe,
    TableModule, ButtonModule, RippleModule, BreadcrumbModule,
    ToastModule, TooltipModule, TagModule, CalendarModule, ConfirmDialogModule
  ],
  templateUrl: './agios-calcul-detail.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosCalculDetailComponent implements OnInit, OnDestroy {

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

  // Pagination
  rows = 31;
  first = 0;

  // Date limite filter
  dateLimite?: Date;
  minDate!: Date;
  maxDate!: Date;

  // Search DTO (réutilisé pour table + agrégats)
  private journalierDto!: AgiosJournalierSearchDto;

  // Computed
  banqueNom = '';
  typeLabel = '';

  // Breadcrumb
  items: MenuItem[] = [];

  // Template refs
  readonly StatutTrimestre = StatutTrimestre;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private agiosCalculService: AgiosCalculService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.banqueId = +params['banqueId'];
        this.typeAgios = params['type'] as TypeAgios;
        this.trimestre = +params['trim'];
        this.annee = +params['annee'];
        this.typeLabel = TypeAgiosLabels[this.typeAgios] || this.typeAgios;

        // Bornes du trimestre pour le calendrier
        const moisDebut = (this.trimestre - 1) * 3;
        this.minDate = new Date(this.annee, moisDebut, 1);
        this.maxDate = new Date(this.annee, moisDebut + 3, 0); // dernier jour du trimestre

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
          { label: `T${this.trimestre}/${this.annee}` }
        ];

        this.loadTrimestreInfo();
        this.loadAggregates();
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
    if (this.dateLimite) {
      const d = this.dateLimite;
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
      sortField: 'dateValeur',
      sortOrder: 1
    });
    this.loadAggregates();
  }

  clearDateLimite(dt: Table): void {
    this.dateLimite = undefined;
    this.onDateLimiteChange(dt);
  }

  // ═══════════════════════════════════════════════════════════════
  //  ACTIONS
  // ═══════════════════════════════════════════════════════════════

  cloturer(): void {
    if (!this.trimestreInfo) return;

    this.confirmationService.confirm({
      message: `Clôturer définitivement ${this.typeLabel} T${this.trimestre}/${this.annee} pour ${this.banqueNom} ?
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
              detail: `T${result.trimestre}/${result.annee} clôturé — TTC: ${result.totalAgiosTTC.toFixed(2)} MAD`
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

  /** Vrai si le type d'agios courant est Préfinancement (multi-marché). */
  get isPrefi(): boolean {
    return this.typeAgios === TypeAgios.PREFINANCEMENT;
  }

  /** Libellé de la colonne "Solde" : encours pour PREFI, solde comptable pour FC. */
  get soldeColLabel(): string {
    return this.isPrefi ? 'Encours' : 'Solde';
  }

  /** Libellé du KPI "jours actifs" : encours pour PREFI, découvert pour FC. */
  get joursActifsLabel(): string {
    return this.isPrefi ? 'JOURS ENCOURS' : 'JOURS DÉC.';
  }
}
