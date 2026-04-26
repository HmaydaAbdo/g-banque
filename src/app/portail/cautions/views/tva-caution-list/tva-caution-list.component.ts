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

import { ImpotEtTaxeService } from '../../../impotEtTaxes/service/ImpotEtTaxeService';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from '../../../../auth/services/auth.service';
import { ImpotEtTaxeResponseDto } from '../../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto';
import { ImpotEtTaxeSearchDto } from '../../../impotEtTaxes/dtos/ImpotEtTaxeSearchDto';
import { Motif } from '../../../impotEtTaxes/dtos/Motif';
import { Status } from '../../../prefinancements/enums/Status';

@Component({
  selector: 'app-tva-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, ButtonModule, DropdownModule,
    InputTextModule, TagModule, ToastModule, ToolbarModule, TooltipModule,
    BreadcrumbModule, ConfirmDialogModule, DatePipe, DecimalPipe,
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
  tvas: ImpotEtTaxeResponseDto[] = [];
  totalRecords = 0;
  loading      = false;

  readonly isAdmin = this.authService.isAdmin();

  // ── Options dropdowns ─────────────────────────────────────────────────────
  banques: any[] = [];

  readonly statusOptions = [
    { label: 'Tous',   value: null          },
    { label: 'Prévu',  value: Status.PREVU  },
    { label: 'Payé',   value: Status.PAYE   },
  ];

  // ── Reactive filter form ──────────────────────────────────────────────────
  filterForm: FormGroup = this.fb.group({
    banqueId: [null],
    libelle:  [null],
    status:   [null],
  });

  breadcrumbItems: MenuItem[] = [];

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
        error: () => this.messageService.add({
          severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les banques.',
        }),
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

    const { banqueId, libelle, status } = this.filterForm.value;

    // motif TVA_COMMISSION_CAUTION est toujours fixé — filtre opaque pour l'utilisateur
    const searchDto: ImpotEtTaxeSearchDto = {
      motif: Motif.TVA_COMMISSION_CAUTION,
      ...(banqueId && { banqueId }),
      ...(libelle  && { libelle  }),
      ...(status   && { status   }),
    };

    this.tvaService.searchImpotEtTaxes(searchDto, page, size, sort)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.tvas         = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading      = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.loading = false;
          this.cdr.markForCheck();
          this.messageService.add({
            severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les TVA.',
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
              // Mise à jour locale de la ligne — pas de rechargement complet
              tva.status = versPaye ? Status.PAYE : Status.PREVU;
              this.tvas  = [...this.tvas];
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
              this.tvas         = this.tvas.filter(t => t.id !== tva.id);
              this.totalRecords = this.totalRecords - 1;
              this.cdr.markForCheck();
              this.messageService.add({
                severity: 'success', summary: 'Supprimée', detail: 'TVA supprimée avec succès.',
              });
            },
            error: err => this.messageService.add({
              severity: 'error', summary: 'Erreur',
              detail: err.error?.message ?? 'Impossible de supprimer la TVA.',
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
