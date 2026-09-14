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
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { Ripple } from 'primeng/ripple';

import { BehaviorSubject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { CautionService } from '../../service/caution.service';
import { CautionResponseDto } from '../../dtos/CautionResponseDto';
import { CautionSearchDto } from '../../dtos/CautionSearchDto';
import { CloturerCautionRequest } from '../../dtos/CloturerCautionRequest';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';
import { AuthService } from '../../../../auth/services/auth.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { NatureLigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service';

const MARCHE_PAGE_SIZE = 15;

@Component({
  selector: 'app-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, RouterLink, TableModule, ButtonModule, CheckboxModule,
    DialogModule, OverlayPanelModule, DropdownModule, InputTextModule, TagModule, ToastModule, ToolbarModule,
    TooltipModule, BreadcrumbModule, ConfirmDialogModule, CalendarModule, InputNumberModule,
    DecimalPipe, DatePipe, Ripple,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './caution-list.component.html',
})
export class CautionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;
  @ViewChild('exportColumnsPanel') exportColumnsPanel!: OverlayPanel;

  private readonly fb                  = inject(FormBuilder);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly cdr                 = inject(ChangeDetectorRef);
  readonly cautionService              = inject(CautionService);
  private readonly authService         = inject(AuthService);
  private readonly banqueService       = inject(BanqueService);
  private readonly marcheService       = inject(MarcheService);
  private readonly natureService       = inject(NatureLigneFonctionnelleService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  readonly cautions$      = this.cautionService.cautions$;
  readonly totalElements$ = this.cautionService.totalElements$;
  readonly loading$       = this.cautionService.loading$;

  readonly isSystemAdmin = this.authService.isSystemAdmin();
  readonly today   = new Date();

  /** Indicateur de chargement du bouton "Exporter PDF". */
  exporting = false;

  /**
   * Colonnes exportables dans le PDF de la liste des cautions — clés et ordre alignés sur
   * `CAUTION_EXPORT_COLUMNS` côté backend (`CautionServiceImpl`). Toutes cochées par défaut.
   */
  exportColumns: { key: string; label: string; checked: boolean }[] = [
    { key: 'numeroCaution',    label: 'N° Caution',     checked: true },
    { key: 'banque',           label: 'Banque',          checked: true },
    { key: 'nature',           label: 'Nature',          checked: true },
    { key: 'marche',           label: 'N° Marché',       checked: true },
    { key: 'organisme',        label: 'Organisme',       checked: true },
    { key: 'designation',      label: 'Désignation',     checked: true },
    { key: 'dateDePrise',      label: 'Date de prise',   checked: true },
    { key: 'dateDeMainLevee',  label: 'Main levée',      checked: true },
    { key: 'montant',          label: 'Montant (MAD)',   checked: true },
    { key: 'taux',             label: 'Taux (%)',        checked: true },
    { key: 'statut',           label: 'Statut',          checked: true },
  ];

  get hasSelectedColumns(): boolean {
    return this.exportColumns.some(c => c.checked);
  }

  // ── Reactive filter form ──────────────────────────────────────────────────
  filterForm: FormGroup = this.fb.group({
    numeroCaution:         [null],
    organisme:             [null],
    montant:               [null],
    actif:                 [null],
    banque:                [null],
    nature:                [null],
    marcheId:              [null],
    dateDePriseDebut:      [null],
    dateDePriseFin:        [null],
    dateDeMainLeveeDebut:  [null],
    dateDeMainLeveeFin:    [null],
  });

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.filterForm.value;
    let count = 0;
    if (v.dateDePriseDebut || v.dateDePriseFin) count++;
    if (v.dateDeMainLeveeDebut || v.dateDeMainLeveeFin) count++;
    return count;
  }

  // ── Static options ────────────────────────────────────────────────────────
  banques:      any[] = [];
  natures:      any[] = [];
  actifOptions        = [
    { label: 'Active',   value: true  },
    { label: 'Inactive', value: false },
  ];

  // ── Marché lazy / virtual-scroll options ──────────────────────────────────
  marchesOptions: any[]     = [];
  marchesLoading            = false;
  /** Pages already fetched — prevents duplicate HTTP calls */
  private loadedMarchePages = new Set<number>();
  private readonly marcheFilter$ = new BehaviorSubject<string>('');

  marchesScrollerOptions: ScrollerOptions = { delay: 250, showLoader: true, lazy: true };

  breadcrumbItems: MenuItem[] = [];

  // ── Clôture dialog ────────────────────────────────────────────────────────
  showCloturerDialog  = false;
  cloturerLoading     = false;
  cloturerTarget: CautionResponseDto | null = null;
  cloturerForm: FormGroup = this.fb.group({
    dateDeMainLevee: [null, Validators.required],
  });

  // ─────────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { label: 'Cautions' },
    ];

    this.loadStaticOptions();
    this.setupMarcheStream();

    // Auto-refresh table when filter form changes
    this.filterForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.applyFilters());
  }

  // ── Static options ────────────────────────────────────────────────────────

  private loadStaticOptions(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.banques = res.content;
          this.cdr.markForCheck();
        },
      });

    this.natureService.getAllNatureLigneFonctionnellesList()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.natures = res;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Marché stream (filter + initial load) ─────────────────────────────────

  private setupMarcheStream(): void {

    this.marcheFilter$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(filter => {
        // Reset page tracking when filter changes so all pages reload
        this.loadedMarchePages.clear();
        return this.loadMarchesData(0, filter);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
      this.initializeMarchesOptions(res);
      this.cdr.markForCheck();
    });
  }

  private loadMarchesData(page: number, filter = '') {
    const criteria = filter ? { numero: filter } : {};
    return this.marcheService.getAllMarches(criteria, page, MARCHE_PAGE_SIZE, 'numero,asc').pipe(
      catchError(() => {
        return of({ content: [], page: { totalElements: 0 } });
      }),
    );
  }

  private initializeMarchesOptions(response: any): void {
    const total = response.page.totalElements;
    const data  = response.content.map((m: any) => ({ label: m.numero, value: m.id }));

    this.marchesOptions = Array.from({ length: total }, () => ({}));
    this.marchesOptions.splice(0, data.length, ...data);
  }

  // ── Marché lazy load (virtual scroll) ────────────────────────────────────

  onLazyLoadMarches(event: any): void {
    const first = event?.first ?? 0;
    const page  = Math.floor(first / MARCHE_PAGE_SIZE);


    if (this.loadedMarchePages.has(page)) return;
    this.loadedMarchePages.add(page);

    this.marchesLoading = true;

    this.loadMarchesData(page, this.marcheFilter$.value)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const newItems = res.content.map((m: any) => ({ label: m.numero, value: m.id }));
          // Splice into a new array reference so Angular's OnPush detects the change
          const updated = [...this.marchesOptions];
          updated.splice(first, newItems.length, ...newItems);
          this.marchesOptions = updated;
          this.marchesLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loadedMarchePages.delete(page);
          this.marchesLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onFilterMarches(event: any): void {
    this.marcheFilter$.next(event.filter?.trim() ?? '');
  }

  // ── Table lazy load ───────────────────────────────────────────────────────

  loadCautions(event: TableLazyLoadEvent): void {
    this.cautionService.setLoading(true);

    const page = event.first ? Math.floor(event.first / (event.rows ?? 10)) : 0;

    const criteres: CautionSearchDto = {
      ...this.buildFilterCriteres(),
      page,
      size:  event.rows ?? 10,
      sort: `${event.sortField ?? 'id'},${event.sortOrder === 1 ? 'asc' : 'desc'}`,
    };

    this.cautionService.rechercher(criteres)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.cautionService.set(res.content);
          this.cautionService.setTotal(res.page.totalElements);
          this.cautionService.setLoading(false);
        },
        error: () => {
          this.cautionService.setLoading(false);
        },
      });
  }

  /** Critères de filtrage courants (sans pagination) — partagés entre la recherche et l'export PDF. */
  private buildFilterCriteres(): CautionSearchDto {
    const formVal = this.filterForm.value;
    return {
      numeroCaution:         formVal.numeroCaution         || undefined,
      organisme:             formVal.organisme             || undefined,
      montant:               formVal.montant               || undefined,
      dateDePriseDebut:      formVal.dateDePriseDebut       ? toLocalDateString(formVal.dateDePriseDebut) : undefined,
      dateDePriseFin:        formVal.dateDePriseFin         ? toLocalDateString(formVal.dateDePriseFin) : undefined,
      dateDeMainLeveeDebut:  formVal.dateDeMainLeveeDebut   ? toLocalDateString(formVal.dateDeMainLeveeDebut) : undefined,
      dateDeMainLeveeFin:    formVal.dateDeMainLeveeFin     ? toLocalDateString(formVal.dateDeMainLeveeFin) : undefined,
      banqueId:              formVal.banque?.id,
      natureId:              formVal.nature?.id,
      marcheId:              formVal.marcheId              || undefined,
      actif:                 formVal.actif !== null ? formVal.actif : undefined,
    };
  }

  /** Ouvre/ferme le panneau de sélection des colonnes, ancré au bouton "Exporter PDF". */
  toggleExportPanel(event: Event): void {
    this.exportColumnsPanel.toggle(event);
  }

  toggleAllColumns(checked: boolean): void {
    this.exportColumns.forEach(c => c.checked = checked);
  }

  /**
   * Exporte en PDF l'intégralité des cautions correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran — en ne retenant que les colonnes
   * cochées dans la boîte de dialogue.
   */
  confirmExport(): void {
    if (!this.hasSelectedColumns) return;

    this.exportColumnsPanel.hide();
    this.exporting = true;

    const colonnes = this.exportColumns.filter(c => c.checked).map(c => c.key);

    this.cautionService.exportPdf(this.buildFilterCriteres(), colonnes)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `cautions_${new Date().toISOString().slice(0, 10)}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
          this.exporting = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.exporting = false;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Filters ───────────────────────────────────────────────────────────────

  applyFilters(): void { this.table?.reset(); }

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  getActifSeverity(actif: boolean): 'success' | 'danger' {
    return actif ? 'success' : 'danger';
  }

  ouvrirCloturerDialog(caution: CautionResponseDto): void {
    this.cloturerTarget = caution;
    // Pré-remplir la date de main levée si elle existe déjà sur la caution
    this.cloturerForm.reset({
      dateDeMainLevee: caution.dateDeMainLevee ? parseLocalDate(caution.dateDeMainLevee) : null,
    });
    this.showCloturerDialog = true;
    this.cdr.markForCheck();
  }

  confirmerCloture(): void {
    if (this.cloturerForm.invalid || !this.cloturerTarget) return;

    this.cloturerLoading = true;
    this.cdr.markForCheck();

    const request: CloturerCautionRequest = {
      dateDeMainLevee: toLocalDateString(this.cloturerForm.value.dateDeMainLevee)!,
    };

    this.cautionService.cloturer(this.cloturerTarget.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.cautionService.updateInList(res.caution);
          this.cloturerLoading    = false;
          this.showCloturerDialog = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Caution clôturée',
            detail: `Commission générée : ${res.commission.montant.toFixed(2)} MAD`,
            life: 6000,
          });
          this.cdr.markForCheck();
        },
        error: err => {
          this.cloturerLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Échappe les caractères HTML spéciaux avant interpolation dans un message de confirmation
   * PrimeNG (rendu via `[innerHTML]` par `p-confirmDialog`) — `numeroCaution` est une donnée
   * libre saisie par un utilisateur Finance et ne doit jamais être insérée telle quelle dans du
   * HTML (frontend.md, section Sécurité).
   */
  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  supprimer(caution: CautionResponseDto): void {
    const numeroEchappe = this.escapeHtml(caution.numeroCaution);
    this.confirmationService.confirm({
      message: `Supprimer définitivement la caution <strong>${numeroEchappe}</strong> ? Cette action est irréversible.`,
      header: 'Confirmer la suppression',
      icon: 'pi pi-trash',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.cautionService.supprimer(caution.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Supprimée',
                detail: `Caution ${caution.numeroCaution} supprimée avec succès.`,
              });
              this.applyFilters();
            },
          });
      },
    });
  }
}
