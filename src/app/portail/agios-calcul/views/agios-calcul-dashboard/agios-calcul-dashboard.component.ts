import { Component, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, finalize, debounceTime, distinctUntilChanged } from 'rxjs';

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
    CommonModule, ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe,
    TableModule, DropdownModule, InputTextModule, InputNumberModule,
    ButtonModule, RippleModule, BreadcrumbModule, ToastModule,
    TooltipModule, TagModule, ToolbarModule, ConfirmDialogModule, ProgressBarModule
  ],
  templateUrl: './agios-calcul-dashboard.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosCalculDashboardComponent implements OnInit, OnDestroy {

  private readonly agiosCalculService = inject(AgiosCalculService);
  private readonly banqueService = inject(BanqueService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private destroy$ = new Subject<void>();

  @ViewChild('dt') dt?: Table;

  // Data
  trimestres: AgiosTrimestrielResponseDto[] = [];
  totalRecords = 0;

  // State
  loading = false;
  recalculEnCours = false;
  exporting = false;

  // Pagination
  rows = 10;
  first = 0;
  sortField = 'annee';
  sortOrder = -1;

  // Filtres (Reactive Forms)
  searchForm: FormGroup = this.fb.group({
    banqueId: [null],
    type: [null],
    annee: [null],
    trimestre: [null],
    statut: [null]
  });

  // Banques dropdown (lazy)
  banques: BanqueResponse[] = [];
  banquesLoading = false;
  banquesTotal = 0;

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

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-calculator', route: '/portail/agios/calcul', label: 'Calcul des Agios' },
    ];

    this.searchForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
      )
      .subscribe(() => this.onFilterChange());
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

    const dto: AgiosTrimestrielSearchDto = this.searchForm.value;

    this.agiosCalculService.searchTrimestres(dto, page, size, sort)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: res => {
          this.trimestres = res.content;
          this.totalRecords = res.page.totalElements;
        },
        error: () => {}
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

  onFilterChange(dt?: Table): void {
    const table = dt ?? this.dt;
    this.first = 0;
    if (table) {
      table.first = 0;
      table.onLazyLoad.emit({
        first: 0,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder
      });
    } else {
      this.loadTrimestres({
        first: 0,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder
      });
    }
  }

  clearFilters(dt: Table): void {
    this.searchForm.reset();
    this.onFilterChange(dt);
  }

  exportPdf(): void {
    this.exporting = true;
    const dto: AgiosTrimestrielSearchDto = this.searchForm.value;

    this.agiosCalculService.exportTrimestresPdf(dto)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.exporting = false)
      )
      .subscribe({
        next: (blob: Blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `agios_calcul_trimestres_${new Date().toISOString().slice(0, 10)}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        }
      });
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
            error: () => {}
          });
      }
    });
  }

  recalculerParBanque(trim: AgiosTrimestrielResponseDto, dt: Table): void {
    this.confirmationService.confirm({
      message: `Recalculer les agios <strong>${TypeAgiosLabels[trim.typeAgios]}</strong> T${trim.trimestre}/${trim.annee} pour <strong>${trim.banqueNom}</strong> ?`,
      header: 'Confirmer le recalcul',
      icon: 'pi pi-refresh',
      acceptLabel: 'Recalculer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => {
        this.loading = true;
        this.agiosCalculService.recalculerParBanque(trim.banqueId, trim.trimestre, trim.annee)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.loading = false)
          )
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success', summary: 'Recalcul terminé',
                detail: `T${trim.trimestre}/${trim.annee} — ${trim.banqueNom} recalculé avec succès.`
              });
              this.onFilterChange(dt);
            },
            error: () => {}
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
          error: () => {}
        });
      }
    });
  }

  voirDetail(trim: AgiosTrimestrielResponseDto): void {
    this.router.navigate([
      '/portail/agios/calcul/detail',
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
