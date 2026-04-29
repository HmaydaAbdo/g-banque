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
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
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
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { Status } from '../../../prefinancements/enums/Status';

@Component({
  selector: 'app-commission-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, ButtonModule, DropdownModule,
    InputTextModule, TagModule, ToastModule, ToolbarModule, TooltipModule,
    BreadcrumbModule, ConfirmDialogModule, DatePipe, DecimalPipe,
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
  commissions: CommissionResponseDto[] = [];
  totalRecords = 0;
  loading      = false;

  readonly isAdmin = this.authService.isAdmin();

  // ── Options dropdowns ─────────────────────────────────────────────────────
  banques: any[] = [];



  // ── Reactive filter form ──────────────────────────────────────────────────
  filterForm: FormGroup = this.fb.group({
    banqueId: [null],
    libelle:  [null],
  });

  breadcrumbItems: MenuItem[] = [];

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
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les banques.',
        }),
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

    const { banqueId, libelle } = this.filterForm.value;

    // operation CAUTION est toujours fixé — ces commissions ne concernent que les cautions
    const searchDto: CommissionSearchDto = {
      operation: TransactionSource.CAUTION,
      status:Status.PREVU,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
    };

    this.commissionService.searchCommissions(searchDto, page, size, sort)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.commissions  = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading      = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.cdr.markForCheck();
          this.messageService.add({
            severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les commissions.',
          });
        },
      });
  }

  // ── Filtres ───────────────────────────────────────────────────────────────
  applyFilters(): void { this.table?.reset(); }

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  // ── Toggle statut ─────────────────────────────────────────────────────────
  toggleStatut(commission: CommissionResponseDto): void {
    const versPaye = commission.status === Status.PREVU;
    this.confirmationService.confirm({
      message: versPaye
        ? `Marquer la commission <strong>${commission.libelle}</strong> comme payée ?`
        : `Repasser la commission <strong>${commission.libelle}</strong> en prévu ?`,
      header: 'Confirmer',
      icon: 'pi pi-info-circle',
      acceptLabel: versPaye ? 'Marquer payée' : 'Repasser en prévu',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: versPaye ? 'p-button-success' : 'p-button-warning',
      accept: () => {
        this.commissionService.toggleCommissionStatus(commission.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              // Mise à jour locale — pas de rechargement complet
              commission.status = versPaye ? Status.PAYE : Status.PREVU;
              this.commissions  = [...this.commissions];
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.',
              });
            },
            error: err => this.messageService.add({
              severity: 'error', summary: 'Erreur',
              detail: err.error?.message ?? 'Impossible de modifier le statut.',
            }),
          });
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
              this.commissions  = this.commissions.filter(c => c.id !== commission.id);
              this.totalRecords = this.totalRecords - 1;
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Supprimée', detail: `Commission supprimée avec succès.`,
              });
            },
            error: err => this.messageService.add({
              severity: 'error', summary: 'Erreur',
              detail: err.error?.message ?? 'Impossible de supprimer la commission.',
            }),
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
