import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

// PrimeNG
import { TableModule, Table, TableLazyLoadEvent } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { MessageService, ConfirmationService, MenuItem } from 'primeng/api';

// App
import { AgiosCalculService } from '../../service/agios-calcul.service';
import { AgiosTrimestrielResponseDto } from '../../dtos/AgiosTrimestrielResponseDto';
import { AgiosTrimestrielSearchDto } from '../../dtos/AgiosTrimestrielSearchDto';
import { StatutTrimestre } from '../../enums/StatutTrimestre';
import { TypeAgios, TypeAgiosLabels } from '../../enums/TypeAgios';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { AuthService } from '../../../../auth/services/auth.service';

@Component({
  selector: 'app-agios-calcul-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe,
    TableModule, DropdownModule, InputTextModule, InputNumberModule,
    ButtonModule, RippleModule, BreadcrumbModule, ToastModule,
    TooltipModule, TagModule, ToolbarModule, ConfirmDialogModule, ProgressBarModule
  ],
  templateUrl: './agios-calcul-dashboard.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosCalculDashboardComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject<void>();

  // Data
  trimestres: AgiosTrimestrielResponseDto[] = [];
  totalRecords = 0;

  // State
  loading = false;
  recalculEnCours = false;

  // Pagination
  rows = 10;
  first = 0;
  sortField = 'annee';
  sortOrder = -1;

  // Search DTO
  searchDto: AgiosTrimestrielSearchDto = {};

  // Banques dropdown (lazy)
  banques: BanqueResponse[] = [];
  banquesLoading = false;
  banquesTotal = 0;
  selectedBanque?: BanqueResponse;

  // Année (saisie libre)
  anneeInput?: number;

  // Dropdown options
  typeOptions: { label: string; value: TypeAgios }[] = Object.values(TypeAgios).map(t => ({
    label: TypeAgiosLabels[t],
    value: t
  }));

  trimestreOptions = [
    { label: 'T1', value: 1 },
    { label: 'T2', value: 2 },
    { label: 'T3', value: 3 },
    { label: 'T4', value: 4 },
  ];

  statutOptions = [
    { label: 'En cours', value: StatutTrimestre.OUVERT },
    { label: 'Clôturé', value: StatutTrimestre.CLOTURE },
  ];

  // Breadcrumb
  items: MenuItem[] = [];

  // Template refs
  readonly StatutTrimestre = StatutTrimestre;
  readonly TypeAgiosLabels = TypeAgiosLabels;

  constructor(
    private agiosCalculService: AgiosCalculService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-calculator', route: '/portail/agios-calcul', label: 'Calcul des Agios' },
    ];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ═══════════════════════════════════════════════════════════════
  //  LAZY LOADING — SERVER-SIDE
  // ═══════════════════════════════════════════════════════════════

  loadTrimestres(event: TableLazyLoadEvent): void {
    this.loading = true;

    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    const size = event.rows ?? this.rows;

    let sort = 'annee,desc';
    if (event.sortField) {
      const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      const order = event.sortOrder === 1 ? 'asc' : 'desc';
      sort = `${field},${order}`;
    }

    this.agiosCalculService.searchTrimestres(this.searchDto, page, size, sort)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: res => {
          this.trimestres = res.content;
          this.totalRecords = res.page.totalElements;
        },
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur',
          detail: 'Impossible de charger les trimestres.'
        })
      });
  }

  // ═══════════════════════════════════════════════════════════════
  //  BANQUES — LAZY DROPDOWN
  // ═══════════════════════════════════════════════════════════════

  loadBanquesLazy(event: any): void {
    this.banquesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;

    this.banqueService.getAllBanques({}, page, size, 'nom,asc')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: res => {
          this.banques = res.content;
          this.banquesTotal = res.page.totalElements;
          this.banquesLoading = false;
        },
        error: () => this.banquesLoading = false
      });
  }

  // ═══════════════════════════════════════════════════════════════
  //  FILTRAGE — DÉCLENCHE LE RELOAD SERVEUR
  // ═══════════════════════════════════════════════════════════════

  onBanqueChange(dt: Table): void {
    this.searchDto.banqueId = this.selectedBanque?.id ?? undefined;
    this.onFilterChange(dt);
  }

  onAnneeChange(dt: Table): void {
    this.searchDto.annee = this.anneeInput ?? undefined;
    this.onFilterChange(dt);
  }

  onFilterChange(dt: Table): void {
    this.first = 0;
    dt.first = 0;
    dt.onLazyLoad.emit({
      first: 0,
      rows: this.rows,
      sortField: this.sortField,
      sortOrder: this.sortOrder
    });
  }

  clearFilters(dt: Table): void {
    this.searchDto = {};
    this.selectedBanque = undefined;
    this.anneeInput = undefined;
    this.onFilterChange(dt);
  }

  // ═══════════════════════════════════════════════════════════════
  //  ACTIONS
  // ═══════════════════════════════════════════════════════════════

  recalculer(dt: Table): void {
    this.confirmationService.confirm({
      message: 'Lancer le recalcul de tous les agios pour toutes les banques ? Cette opération peut prendre quelques minutes.',
      header: 'Confirmer le recalcul',
      icon: 'pi pi-refresh',
      accept: () => {
        this.recalculEnCours = true;
        this.agiosCalculService.recalculerTout()
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.recalculEnCours = false)
          )
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success', summary: 'Succès',
                detail: 'Recalcul terminé avec succès.'
              });
              this.onFilterChange(dt);
            },
            error: () => this.messageService.add({
              severity: 'error', summary: 'Erreur',
              detail: 'Le recalcul a échoué.'
            })
          });
      }
    });
  }

  cloturer(trim: AgiosTrimestrielResponseDto, dt: Table): void {
    this.confirmationService.confirm({
      message: `Clôturer définitivement ${TypeAgiosLabels[trim.typeAgios]} T${trim.trimestre}/${trim.annee} pour ${trim.banqueNom} ?\nMontant TTC: ${trim.totalAgiosTTC.toFixed(2)} MAD — un agios PREVU sera créé.`,
      header: 'Confirmer la clôture',
      icon: 'pi pi-lock',
      acceptLabel: 'Clôturer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.loading = true;
        this.agiosCalculService.cloturerTrimestre(
          trim.banqueId, trim.typeAgios, trim.trimestre, trim.annee
        ).pipe(
          takeUntil(this.destroy$),
          finalize(() => this.loading = false)
        ).subscribe({
          next: result => {
            this.messageService.add({
              severity: 'success', summary: 'Clôturé',
              detail: `T${result.trimestre}/${result.annee} clôturé — TTC: ${result.totalAgiosTTC.toFixed(2)} MAD`
            });
            this.onFilterChange(dt);
          },
          error: () => this.messageService.add({
            severity: 'error', summary: 'Erreur',
            detail: 'La clôture a échoué.'
          })
        });
      }
    });
  }

  voirDetail(trim: AgiosTrimestrielResponseDto): void {
    this.router.navigate([
      '/portail/agios-calcul/detail',
      trim.banqueId, trim.typeAgios, trim.trimestre, trim.annee
    ]);
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

  getTypeLabel(trim: AgiosTrimestrielResponseDto): string {
    return TypeAgiosLabels[trim.typeAgios] || trim.typeAgios;
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}
