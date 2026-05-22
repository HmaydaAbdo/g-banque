import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';

import { BehaviorSubject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { CautionService } from '../../service/caution.service';
import { CautionRequestDto } from '../../dtos/CautionRequestDto';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { NatureLigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

const MARCHE_PAGE_SIZE = 10;

@Component({
  selector: 'app-caution-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    InputTextModule, InputTextareaModule, InputNumberModule,
    DropdownModule, CalendarModule, ButtonModule, RippleModule,
    ToastModule, ConfirmDialogModule, BreadcrumbModule, TooltipModule,
  ],
  templateUrl: './caution-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class CautionFormComponent implements OnInit {

  private readonly destroyRef         = inject(DestroyRef);
  private readonly fb                 = inject(FormBuilder);
  private readonly route              = inject(ActivatedRoute);
  private readonly location           = inject(Location);
  private readonly cdr                = inject(ChangeDetectorRef);
  private readonly cautionService     = inject(CautionService);
  private readonly banqueService      = inject(BanqueService);
  private readonly natureService      = inject(NatureLigneFonctionnelleService);
  private readonly marcheService      = inject(MarcheService);
  private readonly messageService     = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  form!: FormGroup;
  isEditMode = false;
  cautionId: number | null = null;
  loading = false;
  readonly today = new Date();
  marcheRequired = false;

  // ── Listes statiques (courtes, chargées en une seule fois) ────────────────
  banques: any[] = [];
  natures: any[] = [];

  // ── Marché : virtual scroll + lazy + recherche serveur ───────────────────
  marchesOptions: any[]    = [];
  marchesTotalCount        = 0;
  marchesLoading           = false;
  private loadedMarchePages = new Set<number>();
  private readonly marcheFilter$ = new BehaviorSubject<string>('');
  marchesScrollerOptions: ScrollerOptions = { delay: 250, showLoader: true, lazy: true };

  /** Marché pré-sélectionné (mode édition) — affiché pendant le chargement initial */
  private _seededMarcheOption: { label: string | null; value: number } | null = null;

  breadcrumbItems: MenuItem[] = [];

  // ─────────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.initForm();
    this.loadStaticOptions();
    this.setupMarcheStream();
    this.setupRouteListener();
    this.setupNatureListener();
  }

  // ── Formulaire ────────────────────────────────────────────────────────────

  private initForm(): void {
    this.form = this.fb.group({
      banqueId:      [null, Validators.required],
      natureId:      [null, Validators.required],
      marcheId:      [null],
      dateDePrise:      [null, Validators.required],
      dateDeMainLevee:  [null],
      numeroCaution: [null, [Validators.required, Validators.maxLength(100)]],
      designation:   [null, Validators.required],
      organisme:     [null, Validators.required],
      montant:       [null, [Validators.required, Validators.min(0.01)]],
      taux:          [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
  }

  // ── Listes statiques ──────────────────────────────────────────────────────

  private loadStaticOptions(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.banques = res.content.map(b => ({ label: b.nom, value: b.id }));
          this.cdr.markForCheck();
        },
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les banques.',
        }),
      });

    this.natureService.getAllNatureLigneFonctionnellesList()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.natures = res.map(n => ({ label: n.label, value: n.id, code: n.code }));
          this.cdr.markForCheck();
        },
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les natures.',
        }),
      });
  }

  // ── Route ────────────────────────────────────────────────────────────────

  private setupRouteListener(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params: ParamMap) => {
        const id = params.get('id');
        if (id) {
          this.isEditMode = true;
          this.cautionId  = Number(id);
          this.loadCaution(this.cautionId);
        } else {
          this.handlePreselectedBanque();
        }
        this.setupBreadcrumbs();
      });
  }

  private loadCaution(id: number): void {
    this.loading = true;
    this.cautionService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: c => {
          if (!c.actif) {
            this.messageService.add({
              severity: 'warn',
              summary:  'Accès refusé',
              detail:   'Cette caution est archivée et ne peut pas être modifiée.',
            });
            setTimeout(() => this.location.back(), 1500);
            return;
          }

          this.form.patchValue({
            ...c,
            dateDePrise:     parseLocalDate(c.dateDePrise),
            dateDeMainLevee: c.dateDeMainLevee ? parseLocalDate(c.dateDeMainLevee) : null,
          });

          if (c.marcheId) {
            // Seed visible immédiatement avant que le stream ne réponde.
            this._seededMarcheOption = { label: c.marcheNumero, value: c.marcheId };
            this.marchesOptions = [this._seededMarcheOption];
            // Le stream (setupMarcheStream) initialise le tableau sparse via debounce.
            // loadMarchesLazy prend le relais pour les pages suivantes au scroll.
          }

          // Appliquer la règle marché obligatoire selon le code nature chargé
          this.updateMarcheValidator(c.natureCode);

          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => this.location.back(),
      });
  }

  private handlePreselectedBanque(): void {
    const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
    if (banqueIdParam) {
      this.form.patchValue({ banqueId: +banqueIdParam });
    }
  }

  // ── Marché : stream de recherche (filtre serveur) ─────────────────────────

  private setupMarcheStream(): void {
    this.marcheFilter$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(filter => {
        // Réinitialise le tracking des pages chargées à chaque changement de filtre
        this.loadedMarchePages.clear();
        this.marchesLoading = true;
        const criteria = filter ? { numero: filter } : {};
        return this.marcheService.getAllMarches(criteria, 0, MARCHE_PAGE_SIZE, 'numero,asc').pipe(
          catchError(() => {
            this.messageService.add({
              severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les marchés.',
            });
            return of({ content: [], page: { totalElements: 0 } });
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
      const total = (res as any).page.totalElements;
      const firstPageData = (res as any).content.map((m: any) => ({ label: m.numero, value: m.id }));
      const sparse: any[] = Array.from({ length: total }, () => ({}));
      sparse.splice(0, firstPageData.length, ...firstPageData);

      // Mode édition : réinjecter le seed si absent de la page 0
      const seed = this._seededMarcheOption;
      if (seed && !firstPageData.some((d: any) => d.value === seed.value)) {
        sparse[total - 1] = seed;
      }

      this.marchesOptions = sparse;
      this.marchesTotalCount = total;
      this.loadedMarchePages.add(0); // page 0 déjà chargée par le stream
      this.marchesLoading = false;
      this.cdr.markForCheck();
    });
  }

  // ── Marché : lazy scroll (pages 1, 2, …) ─────────────────────────────────

  loadMarchesLazy(event: any): void {
    const first = event?.first ?? 0;
    const page  = Math.floor(first / MARCHE_PAGE_SIZE);

    if (this.loadedMarchePages.has(page)) return;
    this.loadedMarchePages.add(page);

    this.marchesLoading = true;

    const filter   = this.marcheFilter$.value;
    const criteria = filter ? { numero: filter } : {};

    this.marcheService.getAllMarches(criteria, page, MARCHE_PAGE_SIZE, 'numero,asc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const data    = res.content.map(m => ({ label: m.numero, value: m.id }));
          const updated = [...this.marchesOptions];
          updated.splice(first, data.length, ...data);
          this.marchesOptions = updated;
          this.marchesLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loadedMarchePages.delete(page);
          this.marchesLoading = false;
          this.messageService.add({
            severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les marchés.',
          });
          this.cdr.markForCheck();
        },
      });
  }

  onFilterMarches(event: any): void {
    this.marcheFilter$.next(event.filter?.trim() ?? '');
  }

  // ── Règle marché obligatoire selon la nature ─────────────────────────────

  private setupNatureListener(): void {
    this.form.get('natureId')!.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(natureId => {
      const selected = this.natures.find(n => n.value === natureId);
      if (selected) {
        this.updateMarcheValidator(selected.code);
      }
    });
  }

  updateMarcheValidator(natureCode: string): void {
    const marcheControl = this.form.get('marcheId')!;
    if (natureCode === 'CAUT_PROV') {
      this.marcheRequired = false;
      marcheControl.clearValidators();
    } else {
      this.marcheRequired = true;
      marcheControl.setValidators(Validators.required);
    }
    marcheControl.updateValueAndValidity();
    this.cdr.markForCheck();
  }

  // ── Soumission ────────────────────────────────────────────────────────────

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Modifier cette caution ?' : 'Créer cette caution ?',
      accept: () => {
        this.loading = true;
        this.cdr.markForCheck();

        const v = this.form.value;
        const dto: CautionRequestDto = {
          ...v,
          dateDePrise:     toLocalDateString(v.dateDePrise)!,
          dateDeMainLevee: v.dateDeMainLevee ? toLocalDateString(v.dateDeMainLevee) : null,
        };

        const req$ = this.isEditMode
          ? this.cautionService.modifier(this.cautionId!, dto)
          : this.cautionService.creer(dto);

        req$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.loading = false;
            this.messageService.add({ severity: 'success', summary: 'Succès' });
            this.cdr.markForCheck();
            setTimeout(() => this.location.back(), 800);
          },
          error: err => {
            this.loading = false;
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: err.error?.message ?? 'Une erreur est survenue.',
            });
            this.cdr.markForCheck();
          },
        });
      },
    });
  }

  onCancel(): void { this.location.back(); }

  get f() { return this.form.controls; }

  // ── Breadcrumbs ───────────────────────────────────────────────────────────

  private setupBreadcrumbs(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home',   route: '/portail/accueil',  label: 'Accueil'  },
      { icon: 'pi pi-shield', route: '/portail/cautions', label: 'Cautions' },
      { label: this.isEditMode ? 'Modifier' : 'Nouvelle caution' },
    ];
  }
}
