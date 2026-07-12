import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule, CurrencyPipe, DatePipe, DecimalPipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Table, TableLazyLoadEvent, TableModule, TableSelectAllChangeEvent} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {CheckboxModule} from 'primeng/checkbox';
import {DialogModule} from 'primeng/dialog';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';
import {ToolbarModule} from 'primeng/toolbar';
import {TooltipModule} from 'primeng/tooltip';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {debounceTime, distinctUntilChanged} from 'rxjs/operators';

import {ImpotEtTaxeService} from '../../../impotEtTaxes/service/ImpotEtTaxeService';
import {BanqueService} from '../../../comptes-bancaire/services/BanqueService';
import {AuthService} from '../../../../auth/services/auth.service';
import {ImpotEtTaxeResponseDto} from '../../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto';
import {ImpotEtTaxeSearchDto} from '../../../impotEtTaxes/dtos/ImpotEtTaxeSearchDto';
import {ImpotEtTaxeFusionRequest} from '../../../impotEtTaxes/dtos/ImpotEtTaxeFusionRequest';
import {Motif} from '../../../impotEtTaxes/dtos/Motif';
import {Status} from '../../../prefinancements/enums/Status';
import {extractErrorMessage} from "../../../global/types/error.utils";

@Component({
  selector: 'app-tva-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, ButtonModule, CheckboxModule,
    DialogModule, DropdownModule, InputTextModule, TagModule, ToastModule,
    ToolbarModule, TooltipModule, BreadcrumbModule, ConfirmDialogModule,
    DatePipe, DecimalPipe,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './tva-caution-list.component.html',
})
export class TvaCautionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  private readonly fb                  = inject(FormBuilder);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly cdr                 = inject(ChangeDetectorRef);
  private readonly tvaService          = inject(ImpotEtTaxeService);
  private readonly banqueService       = inject(BanqueService);
  private readonly authService         = inject(AuthService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  // ── État local ────────────────────────────────────────────────────────────
  tvas:            ImpotEtTaxeResponseDto[] = [];
  totalRecords     = 0;
  totalMontant     = 0;
  loading          = false;
  selectAllLoading = false;

  readonly isAdmin = this.authService.isSystemAdmin();

  /**
   * Sélection persistante cross-pages.
   * Clé = id de la TVA, valeur = objet DTO.
   */
  private readonly selectionMap = new Map<number, ImpotEtTaxeResponseDto>();

  /**
   * Sélection visible dans la page courante — synchronisée depuis selectionMap
   * après chaque chargement de page.
   */
  currentPageSelection: ImpotEtTaxeResponseDto[] = [];

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
  get selectionCount(): number { return this.selectionMap.size; }
  get canFusionner(): boolean  { return this.selectionMap.size >= 2; }

  /** Vrai quand TOUTES les TVA filtrées sont sélectionnées. */
  get isAllSelected(): boolean {
    return this.selectionMap.size === this.totalRecords && this.totalRecords > 0;
  }

  get fusionMontantTotal(): number {
    if (this.isAllSelected) return this.totalMontant;
    let sum = 0;
    this.selectionMap.forEach(t => sum += t.montant ?? 0);
    return sum;
  }

  // ─────────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home',   route: '/portail/accueil',  label: 'Accueil'          },
      { icon: 'pi pi-shield', route: '/portail/cautions', label: 'Cautions'         },
      {                                                    label: 'TVA sur cautions' },
    ];

    this.loadBanques();

    this.filterForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.applyFilters());
  }

  // ── Chargement des banques ────────────────────────────────────────────────
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
  loadTvas(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.cdr.markForCheck();

    const page      = event.first ? Math.floor(event.first / (event.rows ?? 10)) : 0;
    const size      = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort      = `${sortField},${sortOrder}`;

    const { banqueId, libelle, montant } = this.filterForm.value;

    const searchDto: ImpotEtTaxeSearchDto = {
      motif: Motif.TVA_COMMISSION_CAUTION,
      status: Status.PREVU,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
      ...(montant  && { montant  }),
    };

    this.tvaService.searchImpotEtTaxes(searchDto, page, size, sort)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.tvas         = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalMontant = res.totalDesEncours;
          this.loading      = false;
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
   * Synchronise le selectionMap pour la page courante uniquement.
   */
  onSelectionChange(newPageSelection: ImpotEtTaxeResponseDto[]): void {
    this.tvas.forEach(t => this.selectionMap.delete(t.id));
    newPageSelection.forEach(t => this.selectionMap.set(t.id, t));
    this.currentPageSelection = newPageSelection;
    this.cdr.markForCheck();
  }

  private syncSelectionToCurrentPage(): void {
    this.currentPageSelection = this.tvas.filter(t => this.selectionMap.has(t.id));
  }

  // ── Filtres ───────────────────────────────────────────────────────────────
  applyFilters(): void {
    this.table?.reset();
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.table?.reset();
  }

  // ── Gestion de la sélection ───────────────────────────────────────────────

  /**
   * Déclenché par (selectAllChange) du p-table.
   * Un clic sur la case du header sélectionne TOUTES les pages (pas juste la courante).
   */
  onSelectAllChange(event: TableSelectAllChangeEvent): void {
    if (event.checked) {
      this.selectAllTvas();
    } else {
      this.clearSelection();
    }
  }

  /**
   * Charge TOUTES les TVA filtrées en une requête et les injecte dans selectionMap.
   */
  selectAllTvas(): void {
    if (this.selectAllLoading || this.totalRecords === 0) return;

    this.selectAllLoading = true;
    this.cdr.markForCheck();

    const { banqueId, libelle, montant } = this.filterForm.value;

    const searchDto: ImpotEtTaxeSearchDto = {
      motif: Motif.TVA_COMMISSION_CAUTION,
      status: Status.PREVU,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
      ...(montant  && { montant  }),
    };

    this.tvaService.searchImpotEtTaxes(searchDto, 0, this.totalRecords, 'dateEmission,desc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          res.page.content.forEach(t => this.selectionMap.set(t.id, t));
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

  clearSelection(): void {
    this.selectionMap.clear();
    this.currentPageSelection = [];
    this.cdr.markForCheck();
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

    const request: ImpotEtTaxeFusionRequest = {
      ids:     Array.from(this.selectionMap.keys()),
      libelle: this.fusionForm.value.libelle.trim(),
    };

    this.tvaService.fusionner(request)
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
            detail: `TVA "${res.libelle}" créée (${res.montant.toFixed(2)} MAD).`,
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

  // ── Toggle statut ─────────────────────────────────────────────────────────
  toggleStatut(tva: ImpotEtTaxeResponseDto): void {
    const versPaye = tva.status === Status.PREVU;
    this.confirmationService.confirm({
      message: versPaye
        ? `Marquer la TVA <strong>${tva.libelle}</strong> comme payée ?`
        : `Repasser la TVA <strong>${tva.libelle}</strong> en prévu ?`,
      header: 'Confirmer',
      icon: 'pi pi-info-circle',
      acceptLabel: versPaye ? 'Marquer payée' : 'Repasser en prévu',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: versPaye ? 'p-button-success' : 'p-button-warning',
      accept: () => {
        this.tvaService.toggleStatus(tva.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              tva.status = versPaye ? Status.PAYE : Status.PREVU;
              this.tvas  = [...this.tvas];
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.',
              });
            },
          });
      },
    });
  }

  // ── Suppression ───────────────────────────────────────────────────────────
  supprimer(tva: ImpotEtTaxeResponseDto): void {
    this.confirmationService.confirm({
      message: `Supprimer définitivement la TVA <strong>${tva.libelle}</strong> ?`,
      header: 'Confirmer la suppression',
      icon: 'pi pi-trash',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.tvaService.deleteImpotEtTaxe(tva.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.totalMontant -= tva.montant ?? 0;
              this.tvas          = this.tvas.filter(t => t.id !== tva.id);
              this.totalRecords -= 1;
              this.selectionMap.delete(tva.id);
              this.currentPageSelection = this.currentPageSelection.filter(t => t.id !== tva.id);
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Supprimée', detail: 'TVA supprimée avec succès.',
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
