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
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';

import { CommissionService } from '../../../commission/service/commission.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { CommissionRequestDto } from '../../../commission/dtos/CommissionRequestDto';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { parseLocalDate, toLocalDateString } from '../../../global/types/date.utils';

/**
 * Formulaire d'édition d'une commission sur caution.
 * Route : /portail/cautions/commissions/edit/:id (admin uniquement)
 *
 * Pas de création ici — les commissions caution sont générées automatiquement
 * par le batch mensuel. Ce formulaire permet uniquement la correction manuelle
 * d'une commission existante.
 */
@Component({
  selector: 'app-commission-caution-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    InputTextModule, InputTextareaModule, InputNumberModule,
    DropdownModule, CalendarModule, ButtonModule, RippleModule,
    ToastModule, ConfirmDialogModule, BreadcrumbModule, TooltipModule,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './commission-caution-form.component.html',
})
export class CommissionCautionFormComponent implements OnInit {

  private readonly fb                  = inject(FormBuilder);
  private readonly destroyRef          = inject(DestroyRef);
  private readonly route               = inject(ActivatedRoute);
  private readonly location            = inject(Location);
  private readonly cdr                 = inject(ChangeDetectorRef);
  private readonly commissionService   = inject(CommissionService);
  private readonly banqueService       = inject(BanqueService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  form!: FormGroup;
  commissionId!: number;
  loading = false;

  banques: any[] = [];
  breadcrumbItems: MenuItem[] = [];

  // ─────────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home',   route: '/portail/accueil',              label: 'Accueil'                  },
      { icon: 'pi pi-shield', route: '/portail/cautions',             label: 'Cautions'                 },
      {                       route: '/portail/cautions/commissions',  label: 'Commissions sur cautions' },
      {                                                                label: 'Modifier'                 },
    ];

    this.initForm();
    this.loadBanques();
    this.resolveRoute();
  }

  // ── Formulaire ────────────────────────────────────────────────────────────
  private initForm(): void {
    this.form = this.fb.group({
      banqueId:     [null, Validators.required],
      libelle:      [null, Validators.required],
      montant:      [null, [Validators.required, Validators.min(0.01)]],
      dateEmission: [null, Validators.required],
      observation:  [null],
    });
  }

  // ── Banques (statique) ────────────────────────────────────────────────────
  private loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.banques = res.content.map((b: any) => ({ label: b.nom, value: b.id }));
          this.cdr.markForCheck();
        },
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les banques.',
        }),
      });
  }

  // ── Résolution route + chargement ─────────────────────────────────────────
  private resolveRoute(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params: ParamMap) => {
        const id = params.get('id');
        if (!id) { this.location.back(); return; }
        this.commissionId = Number(id);
        this.loadCommission(this.commissionId);
      });
  }

  private loadCommission(id: number): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.commissionService.getCommissionById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: commission => {
          this.form.patchValue({
            banqueId:     commission.banqueId,
            libelle:      commission.libelle,
            montant:      commission.montant,
            dateEmission: parseLocalDate(commission.dateEmission),
            observation:  commission.observation,
          });
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.messageService.add({
            severity: 'error', summary: 'Erreur', detail: 'Commission introuvable.',
          });
          this.location.back();
        },
      });
  }

  // ── Soumission ────────────────────────────────────────────────────────────
  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.confirmationService.confirm({
      message: 'Modifier cette commission sur caution ?',
      header: 'Confirmer la modification',
      icon: 'pi pi-pencil',
      acceptLabel: 'Modifier',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.cdr.markForCheck();

        const v = this.form.value;
        const dto: CommissionRequestDto = {
          banqueId:     v.banqueId,
          libelle:      v.libelle,
          montant:      v.montant,
          dateEmission: toLocalDateString(v.dateEmission)!,
          observation:  v.observation ?? '',
          // operation toujours CAUTION — la TVA liée sera recalculée côté backend
          operation: TransactionSource.CAUTION,
        };

        this.commissionService.updateCommission(this.commissionId, dto)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.loading = false;
              this.messageService.add({
                severity: 'success', summary: 'Succès', detail: 'Commission mise à jour. La TVA associée a été recalculée.',
              });
              this.cdr.markForCheck();
              setTimeout(() => this.location.back(), 900);
            },
            error: err => {
              this.loading = false;
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'error', summary: 'Erreur',
                detail: err.error?.message ?? 'Impossible de mettre à jour la commission.',
              });
            },
          });
      },
    });
  }

  onCancel(): void { this.location.back(); }

  get f() { return this.form.controls; }
}
