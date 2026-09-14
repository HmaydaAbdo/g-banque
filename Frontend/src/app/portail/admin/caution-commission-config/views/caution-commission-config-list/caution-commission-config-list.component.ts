import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { debounceTime } from 'rxjs/operators';

import { CautionCommissionConfigService } from '../../service/caution-commission-config.service';
import { CautionCommissionConfigResponse } from '../../dtos/CautionCommissionConfigResponse';
import { CautionCommissionConfigRequest } from '../../dtos/CautionCommissionConfigRequest';
import { BanqueService } from '../../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../../global/types/PageResponse';

/**
 * Écran d'administration des paramètres de commission caution (seuil minimum, diviseur
 * du prorata journalier), par banque — réservé SYSTEM_ADMIN.
 *
 * Consomme `/cautions/commission-config` (voir `docs/rbac/caution.md`).
 * Mirroring `CommissionConfigListComponent` (module `commision`), simplifié : pas de
 * type/condition/organisme, une seule configuration par banque (unicité côté backend).
 */
@Component({
  selector: 'app-caution-commission-config-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    TableModule, ButtonModule, RippleModule, DialogModule,
    InputNumberModule, InputTextModule, DropdownModule,
    ToastModule, ConfirmDialogModule, TooltipModule, BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './caution-commission-config-list.component.html'
})
export class CautionCommissionConfigListComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  private svc                 = inject(CautionCommissionConfigService);
  private banqueService       = inject(BanqueService);
  private messageService      = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private fb                  = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home',      route: '/portail/accueil',                          label: 'Accueil' },
    { icon: 'pi pi-shield',    label: 'Administration' },
    { icon: 'pi pi-percentage', route: '/portail/admin/caution-commission-config', label: 'Commissions cautions' }
  ];

  configs: CautionCommissionConfigResponse[] = [];
  banques: BanqueResponse[] = [];

  /**
   * Ids de banques déjà configurées, sur l'ensemble des configurations (pas seulement la
   * page affichée) — nécessaire pour exclure ces banques du formulaire de création
   * (unicité banque/configuration côté backend), indépendamment de la pagination du tableau.
   */
  configuredBanqueIds = new Set<number>();

  totalRecords = 0;
  first = 0;
  rows = 15;
  sortField: string | string[] = 'id';
  sortOrder = 1;

  loading = false;
  dialogVisible = false;
  editingId: number | null = null;

  form!: FormGroup;
  filterForm!: FormGroup;

  ngOnInit(): void {
    this.buildForm();
    this.buildFilterForm();
    this.banqueService.getAllBanques({}, 0, 1000).subscribe(res => this.banques = res.content);
    this.refreshConfiguredBanqueIds();
  }

  private buildFilterForm(): void {
    this.filterForm = this.fb.group({
      search:   [''],
      banqueId: [null]
    });
    this.filterForm.valueChanges.pipe(debounceTime(400)).subscribe(() => this.reloadFromFilters());
  }

  /** Un changement de filtre revient toujours à la page 0. */
  private reloadFromFilters(): void {
    this.first = 0;
    if (this.dt) {
      this.load(this.dt.createLazyLoadMetadata());
    } else {
      this.load();
    }
  }

  clearFilters(): void {
    this.filterForm.reset({ search: '', banqueId: null });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      banqueId:       [null, Validators.required],
      montantMinimum: [null, [Validators.required, Validators.min(0)]],
      diviseur:       [36000, [Validators.required, Validators.min(0.01)]]
    });
  }

  /** Chargement paginé/triable/filtrable, déclenché par le `p-table` en mode `lazy`. */
  load(event: TableLazyLoadEvent = { first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder }): void {
    this.loading = true;

    const first = event.first ?? this.first;
    const rows = event.rows ?? this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === -1 ? -1 : 1;
    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const page = (first as number) / (rows as number);
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    const { search, banqueId } = this.filterForm.value;

    this.svc.findAll({ search, banqueId }, page, rows as number, sort).subscribe({
      next: (res: PageResponse<CautionCommissionConfigResponse>) => {
        this.configs = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  /**
   * Recharge, hors pagination, l'ensemble des ids de banques déjà configurées — utilisé
   * uniquement pour filtrer les options du formulaire de création.
   */
  private refreshConfiguredBanqueIds(): void {
    this.svc.findAll({}, 0, 1000).subscribe(res => {
      this.configuredBanqueIds = new Set(res.content.map(c => c.banqueId));
    });
  }

  get banqueOptions() {
    return this.banques.map(b => ({ label: b.nom, value: b.id }));
  }

  /** Banques n'ayant pas encore de configuration — seules proposées à la création. */
  get availableBanqueOptions() {
    return this.banques
      .filter(b => this.editingId !== null || !this.configuredBanqueIds.has(b.id))
      .map(b => ({ label: b.nom, value: b.id }));
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({ banqueId: null, montantMinimum: null, diviseur: 36000 });
    this.dialogVisible = true;
  }

  openEdit(config: CautionCommissionConfigResponse): void {
    this.editingId = config.id;
    this.form.patchValue({
      banqueId:       config.banqueId,
      montantMinimum: config.montantMinimum,
      diviseur:       config.diviseur
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) return;

    const dto: CautionCommissionConfigRequest = {
      banqueId:       this.form.value.banqueId,
      montantMinimum: this.form.value.montantMinimum,
      diviseur:       this.form.value.diviseur
    };

    const obs = this.editingId
      ? this.svc.update(this.editingId, dto)
      : this.svc.create(dto);

    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Enregistré', detail: 'Configuration sauvegardée.' });
        this.dialogVisible = false;
        this.reloadCurrentPage();
      }
    });
  }

  confirmDelete(config: CautionCommissionConfigResponse, event: Event): void {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Supprimer la configuration pour <b>${config.banqueNom}</b> ? La génération de commission pour cette banque échouera tant qu'aucune nouvelle configuration n'est créée.`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.svc.delete(config.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Supprimé', detail: 'Configuration supprimée.' });
          this.reloadCurrentPage();
        }
      })
    });
  }

  /** Recharge la page actuellement affichée (après create/update/delete), et la liste des ids configurés. */
  private reloadCurrentPage(): void {
    if (this.dt) {
      this.load(this.dt.createLazyLoadMetadata());
    } else {
      this.load();
    }
    this.refreshConfiguredBanqueIds();
  }
}
