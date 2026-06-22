import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe, DecimalPipe, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule, TableSelectAllChangeEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { CommissionService } from '../../../commission/service/commission.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from '../../../../auth/services/auth.service';
import { CommissionResponseDto } from '../../../commission/dtos/commission-response.dto';
import { CommissionSearchDto } from '../../../commission/dtos/CommissionSearchDto';
import { CommissionFusionRequest } from '../../../commission/dtos/CommissionFusionRequest';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { Status } from '../../../prefinancements/enums/Status';
import {extractErrorMessage} from "../../../global/types/error.utils";

@Component({
  selector: 'app-commission-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, ButtonModule, CheckboxModule,
    DialogModule, DropdownModule, InputTextModule, TagModule, ToastModule,
    ToolbarModule, TooltipModule, BreadcrumbModule, ConfirmDialogModule,
    DatePipe, DecimalPipe,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './commission-caution-list.component.html',
})
export class CommissionCautionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  private readonly fb                  = inject(FormBuilder);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly cdr                 = inject(ChangeDetectorRef);
  private readonly router              = inject(Router);
  private readonly commissionService   = inject(CommissionService);
  private readonly banqueService       = inject(BanqueService);
  private readonly authService         = inject(AuthService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  // ── État local ────────────────────────────────────────────────────────────
  commissions:  CommissionResponseDto[] = [];
  totalRecords  = 0;
  totalMontant  = 0;
  loading          = false;
  selectAllLoading = false;

  readonly isAdmin = this.authService.isAdmin();

  /**
   * Sélection persistante cross-pages.
   * Clé = id de la commission, valeur = objet DTO (pour récupérer les montants).
   */
  private readonly selectionMap = new Map<number, CommissionResponseDto>();

  /**
   * Sélection visible dans la page courante — synchronisée depuis selectionMap
   * après chaque chargement de page. C'est ce que p-table "voit".
   */
  currentPageSelection: CommissionResponseDto[] = [];

  // ── Options dropdowns ─────────────────────────────────────────────────────
  banques: any[] = [];

  // ── Reactive filter form ──────────────────────────────────────────────────
  filterForm: FormGroup = this.fb.group({
    banqueId: [null],
    libelle:  [null],
    montant:  [null as number | null],
  });

  // ── Fusion dialog ─────────────────────────────────────────────────────────
  showFusionDialog = false;
  fusionLoading    = false;
  fusionForm: FormGroup = this.fb.group({
    libelle: ['', Validators.required],
  });

  breadcrumbItems: MenuItem[] = [];

  // ── Computed helpers ──────────────────────────────────────────────────────
  private static readonly FACTEUR_TTC = 1.10;

  get selectionCount(): number { return this.selectionMap.size; }

  /** Vrai quand TOUTES les commissions filtrées sont sélectionnées. */
  get isAllSelected(): boolean {
    return this.selectionMap.size === this.totalRecords && this.totalRecords > 0;
  }

  /** Vrai si toutes les commissions sélectionnées appartiennent à la BMCI. */
  get selectionIsBmci(): boolean {
    if (this.selectionMap.size === 0) return false;
    return Array.from(this.selectionMap.values())
      .every(c => c.banqueNom?.toUpperCase().includes('BMCI'));
  }

  /** Fusion autorisée : ≥ 2 commissions ET banque ≠ BMCI. */
  get canFusionner(): boolean { return this.selectionMap.size >= 2 && !this.selectionIsBmci; }
  get fusionMontantTotal(): number {
    // Si toutes les commissions filtrées sont sélectionnées, utiliser le total backend (déjà exact)
    if (this.isAllSelected) return this.totalMontant;
    let sum = 0;
    this.selectionMap.forEach(c => sum += c.montant ?? 0);
    return sum;
  }
  get fusionMontantTotalTTC(): number {
    return this.fusionMontantTotal * CommissionCautionListComponent.FACTEUR_TTC;
  }
  get totalMontantTTC(): number {
    return this.totalMontant * CommissionCautionListComponent.FACTEUR_TTC;
  }

  // ─────────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home',   route: '/portail/accueil',              label: 'Accueil'                  },
      { icon: 'pi pi-shield', route: '/portail/cautions',             label: 'Cautions'                 },
      {                                                                label: 'Commissions sur cautions' },
    ];

    this.loadBanques();

    this.filterForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.applyFilters());
  }

  // ── Chargement des banques ─────────────────────────────────────────────────
  private loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.banques = res.content.map((b: any) => ({ label: b.nom, value: b.id }));
          this.cdr.markForCheck();
        },
      });
  }

  // ── Table lazy load ───────────────────────────────────────────────────────
  loadCommissions(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.cdr.markForCheck();

    const page      = event.first ? Math.floor(event.first / (event.rows ?? 10)) : 0;
    const size      = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort      = `${sortField},${sortOrder}`;

    const { banqueId, libelle, montant } = this.filterForm.value;

    const searchDto: CommissionSearchDto = {
      operation: TransactionSource.CAUTION,
      status: Status.PREVU,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
      ...(montant  && { montant  }),
    };

    this.commissionService.searchCommissions(searchDto, page, size, sort)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.commissions  = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalMontant = res.totalDesEncours;
          this.loading      = false;
          // Re-synchronise les coches pour la page qui vient de charger
          this.syncSelectionToCurrentPage();
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Appelé par (selectionChange) du p-table.
   * Met à jour le selectionMap en tenant compte uniquement des lignes de la page courante :
   * - supprime du map celles de la page qui ont été décochées
   * - ajoute au map celles de la page qui viennent d'être cochées
   * Les sélections des autres pages ne sont pas touchées.
   */
  onSelectionChange(newPageSelection: CommissionResponseDto[]): void {
    // Retirer toutes les lignes de la page courante du map
    this.commissions.forEach(c => this.selectionMap.delete(c.id));
    // Ré-ajouter uniquement celles qui sont cochées
    newPageSelection.forEach(c => this.selectionMap.set(c.id, c));
    this.currentPageSelection = newPageSelection;
    this.cdr.markForCheck();
  }

  /**
   * Recrée currentPageSelection à partir du selectionMap pour les lignes
   * actuellement visibles — appelé après chaque chargement de page.
   */
  private syncSelectionToCurrentPage(): void {
    this.currentPageSelection = this.commissions.filter(c => this.selectionMap.has(c.id));
  }

  // ── Filtres ───────────────────────────────────────────────────────────────
  applyFilters(): void {
    this.selectionMap.clear();
    this.currentPageSelection = [];
    this.table?.reset();
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.selectionMap.clear();
    this.currentPageSelection = [];
    this.table?.reset();
  }

  // ── Gestion de la sélection ───────────────────────────────────────────────

  /**
   * Déclenché par (selectAllChange) du p-table.
   * Un clic sur la case du header sélectionne TOUTES les pages (pas juste la courante).
   */
  onSelectAllChange(event: TableSelectAllChangeEvent): void {
    if (event.checked) {
      this.selectAllCommissions();
    } else {
      this.clearSelection();
    }
  }

  clearSelection(): void {
    this.selectionMap.clear();
    this.currentPageSelection = [];
    this.cdr.markForCheck();
  }

  /**
   * Charge TOUTES les commissions filtrées en une requête et les injecte dans selectionMap.
   * Déclenché par le bandeau "Sélectionner toutes les N commissions" (pattern Gmail).
   */
  selectAllCommissions(): void {
    if (this.selectAllLoading || this.totalRecords === 0) return;

    this.selectAllLoading = true;
    this.cdr.markForCheck();

    const { banqueId, libelle, montant } = this.filterForm.value;
    const searchDto: CommissionSearchDto = {
      operation: TransactionSource.CAUTION,
      status:    Status.PREVU,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
      ...(montant  && { montant  }),
    };

    this.commissionService.searchCommissions(searchDto, 0, this.totalRecords, 'dateEmission,desc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          res.page.content.forEach(c => this.selectionMap.set(c.id, c));
          this.syncSelectionToCurrentPage();
          this.selectAllLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.selectAllLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Fusion ────────────────────────────────────────────────────────────────
  openFusionDialog(): void {
    this.fusionForm.reset({ libelle: '' });
    this.showFusionDialog = true;
    this.cdr.markForCheck();
  }

  confirmerFusion(): void {
    if (this.fusionForm.invalid || !this.canFusionner) return;

    this.fusionLoading = true;
    this.cdr.markForCheck();

    const request: CommissionFusionRequest = {
      ids:     Array.from(this.selectionMap.keys()),
      libelle: this.fusionForm.value.libelle.trim(),
    };

    this.commissionService.fusionner(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.fusionLoading    = false;
          this.showFusionDialog = false;
          this.selectionMap.clear();
          this.currentPageSelection = [];
          this.cdr.markForCheck();

          this.messageService.add({
            severity: 'success',
            summary: 'Fusion réussie',
            detail: `Commission "${res.commission.libelle}" créée (${res.commission.montant.toFixed(2)} MAD).`,
            life: 6000,
          });

          this.table?.reset();
        },
        error: err => {
          this.fusionLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Édition ───────────────────────────────────────────────────────────────
  editer(commission: CommissionResponseDto): void {
    this.router.navigate(['/portail/cautions/commissions/edit', commission.id]);
  }

  // ── Suppression ───────────────────────────────────────────────────────────
  supprimer(commission: CommissionResponseDto): void {
    this.confirmationService.confirm({
      message: `Supprimer définitivement la commission <strong>${commission.libelle}</strong> ?`,
      header: 'Confirmer la suppression',
      icon: 'pi pi-trash',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.commissionService.deleteCommission(commission.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.totalMontant -= commission.montant ?? 0;
              this.commissions   = this.commissions.filter(c => c.id !== commission.id);
              this.totalRecords -= 1;
              this.selectionMap.delete(commission.id);
              this.currentPageSelection = this.currentPageSelection.filter(c => c.id !== commission.id);
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Supprimée', detail: 'Commission supprimée avec succès.',
              });
            },
          });
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  getStatusSeverity(status: Status): 'success' | 'danger' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  getStatusLabel(status: Status): string {
    return status === Status.PAYE ? 'Payée' : 'Prévue';
  }

  protected readonly Status = Status;
}
