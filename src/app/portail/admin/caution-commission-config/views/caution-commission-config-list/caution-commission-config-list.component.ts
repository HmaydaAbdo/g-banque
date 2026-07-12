import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
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
import { forkJoin } from 'rxjs';

import { CautionCommissionConfigService } from '../../service/caution-commission-config.service';
import { CautionCommissionConfigResponse } from '../../dtos/CautionCommissionConfigResponse';
import { CautionCommissionConfigRequest } from '../../dtos/CautionCommissionConfigRequest';
import { BanqueService } from '../../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../../comptes-bancaire/dtos/BanqueResponse';

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

  configs: CautionCommissionConfigResponse[]         = [];
  filteredConfigs: CautionCommissionConfigResponse[] = [];
  banques: BanqueResponse[] = [];

  loading = false;
  dialogVisible = false;
  editingId: number | null = null;

  form!: FormGroup;
  filterForm!: FormGroup;

  ngOnInit(): void {
    this.buildForm();
    this.buildFilterForm();
    this.loadAll();
  }

  private buildFilterForm(): void {
    this.filterForm = this.fb.group({
      search:   [''],
      banqueId: [null]
    });
    this.filterForm.valueChanges.subscribe(() => this.applyFilters());
  }

  /** Filtre côté client — la table des configurations reste de taille modeste (une par banque). */
  applyFilters(): void {
    const { search, banqueId } = this.filterForm.value;
    const term = (search || '').trim().toLowerCase();

    this.filteredConfigs = this.configs.filter(c => {
      if (banqueId && c.banqueId !== banqueId) return false;
      if (term && !c.banqueNom.toLowerCase().includes(term)) return false;
      return true;
    });
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

  /** Charge les configurations et les banques en parallèle. */
  loadAll(): void {
    this.loading = true;
    forkJoin({
      configs: this.svc.findAll(),
      banques: this.banqueService.getAllBanques({}, 0, 1000)
    }).subscribe({
      next: ({ configs, banques }) => {
        this.configs = configs;
        this.banques = banques.content;
        this.loading = false;
        this.applyFilters();
      },
      error: () => { this.loading = false; }
    });
  }

  /** Recharge uniquement la liste des configurations (après create/update/delete). */
  load(): void {
    this.loading = true;
    this.svc.findAll().subscribe({
      next: data => { this.configs = data; this.loading = false; this.applyFilters(); },
      error: ()   => { this.loading = false; }
    });
  }

  get banqueOptions() {
    return this.banques.map(b => ({ label: b.nom, value: b.id }));
  }

  /** Banques n'ayant pas encore de configuration — seules proposées à la création. */
  get availableBanqueOptions() {
    const configuredIds = new Set(this.configs.map(c => c.banqueId));
    return this.banques
      .filter(b => this.editingId !== null || !configuredIds.has(b.id))
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
        this.load();
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
          this.load();
        }
      })
    });
  }
}
