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
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { debounceTime } from 'rxjs/operators';

import { ChargeFixeConfigService } from '../../service/charge-fixe-config.service';
import { ChargeFixeConfigResponse } from '../../dtos/ChargeFixeConfigResponse';
import { ChargeFixeConfigRequest } from '../../dtos/ChargeFixeConfigRequest';
import { TypeChargeFixe, TypeChargeFixeLabels } from '../../dtos/TypeChargeFixe';
import { Motif, MotifLabels } from '../../../../impotEtTaxes/dtos/Motif';
import { BanqueService } from '../../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../../comptes-bancaire/dtos/BanqueResponse';
import {PageResponse} from "../../../../global/types/PageResponse";

/** Mode d'émission choisi dans le formulaire — traduit ensuite vers `jourEmission`. */
type ModeEmission = 'JOUR_FIXE' | 'FIN_MOIS' | 'FIN_TRIMESTRE';

@Component({
  selector: 'app-charge-fixe-config-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    TableModule, ButtonModule, RippleModule, DialogModule,
    InputNumberModule, InputTextModule, DropdownModule, CheckboxModule,
    ToastModule, ConfirmDialogModule, TooltipModule, BreadcrumbModule, TagModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './charge-fixe-config-list.component.html'
})
export class ChargeFixeConfigListComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  private svc            = inject(ChargeFixeConfigService);
  private banqueService  = inject(BanqueService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private fb              = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home',      route: '/portail/accueil',                       label: 'Accueil' },
    { icon: 'pi pi-shield',    label: 'Administration' },
    { icon: 'pi pi-calendar-times', route: '/portail/admin/charges-fixes-config', label: 'Charges fixes config.' }
  ];

  configs: ChargeFixeConfigResponse[] = [];
  banques: BanqueResponse[] = [];

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

  typeOptions = Object.values(TypeChargeFixe).map(v => ({
    label: TypeChargeFixeLabels[v], value: v
  }));

  motifOptions = Object.values(Motif).map(v => ({
    label: MotifLabels[v], value: v
  }));

  modeEmissionOptions: { label: string; value: ModeEmission }[] = [
    { label: 'Jour fixe du mois',           value: 'JOUR_FIXE' },
    { label: 'Dernier jour du mois',        value: 'FIN_MOIS' },
    { label: 'Dernier jour du trimestre',   value: 'FIN_TRIMESTRE' }
  ];

  ngOnInit(): void {
    this.buildForm();
    this.buildFilterForm();
    this.banqueService.getAllBanques({}, 0, 1000).subscribe(res => this.banques = res.content);

    // Réinitialise le motif quand le type change hors IMPOT_TAXE
    // (évite d'envoyer un motif orphelin pour un frais bancaire, agios ou prélèvement).
    this.form.get('type')?.valueChanges.subscribe(type => {
      if (type !== TypeChargeFixe.IMPOT_TAXE) {
        this.form.get('motif')?.reset(null);
      }
    });

    // moisPrecedent n'a de sens que si appendPeriode est coché.
    this.form.get('appendPeriode')?.valueChanges.subscribe(checked => {
      if (!checked) {
        this.form.get('moisPrecedent')?.reset(false);
      }
    });

    // jourFixe n'est pertinent qu'en mode JOUR_FIXE.
    this.form.get('modeEmission')?.valueChanges.subscribe(mode => {
      if (mode !== 'JOUR_FIXE') {
        this.form.get('jourFixe')?.reset(null);
      }
    });
  }

  private buildFilterForm(): void {
    this.filterForm = this.fb.group({
      search:   [''],
      banqueId: [null],
      type:     [null]
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
    this.filterForm.reset({ search: '', banqueId: null, type: null });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      banqueId:      [null, Validators.required],
      libelle:       ['', Validators.required],
      montant:       [null, [Validators.required, Validators.min(0.01)]],
      type:          [null, Validators.required],
      motif:         [null],
      modeEmission:  ['JOUR_FIXE' as ModeEmission, Validators.required],
      jourFixe:      [1, [Validators.min(1), Validators.max(31)]],
      appendPeriode: [false],
      moisPrecedent: [false]
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

    const { search, banqueId, type } = this.filterForm.value;

    this.svc.findAll({ search, banqueId, type }, page, rows as number, sort).subscribe({
      next: (res: PageResponse<ChargeFixeConfigResponse>) => {
        this.configs = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get banqueOptions() {
    return this.banques.map(b => ({ label: b.nom, value: b.id }));
  }

  get isImpotTaxe(): boolean {
    return this.form.get('type')?.value === TypeChargeFixe.IMPOT_TAXE;
  }

  get isJourFixe(): boolean {
    return this.form.get('modeEmission')?.value === 'JOUR_FIXE';
  }

  /** Traduit un `jourEmission` back-end (>0 / -1 / -2) vers le mode + jour du formulaire. */
  private jourEmissionVersFormulaire(jourEmission: number): { modeEmission: ModeEmission; jourFixe: number | null } {
    if (jourEmission === -1) return { modeEmission: 'FIN_MOIS', jourFixe: null };
    if (jourEmission === -2) return { modeEmission: 'FIN_TRIMESTRE', jourFixe: null };
    return { modeEmission: 'JOUR_FIXE', jourFixe: jourEmission };
  }

  /** Traduit le mode + jour du formulaire vers le `jourEmission` attendu par l'API. */
  private formulaireVersJourEmission(): number {
    const { modeEmission, jourFixe } = this.form.value;
    if (modeEmission === 'FIN_MOIS') return -1;
    if (modeEmission === 'FIN_TRIMESTRE') return -2;
    return jourFixe;
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      modeEmission: 'JOUR_FIXE', jourFixe: 1,
      appendPeriode: false, moisPrecedent: false
    });
    this.dialogVisible = true;
  }

  openEdit(config: ChargeFixeConfigResponse): void {
    this.editingId = config.id;
    const { modeEmission, jourFixe } = this.jourEmissionVersFormulaire(config.jourEmission);
    this.form.patchValue({
      banqueId:      config.banqueId,
      libelle:       config.libelle,
      montant:       config.montant,
      type:          config.type,
      motif:         config.motif ?? null,
      modeEmission,
      jourFixe:      jourFixe ?? 1,
      appendPeriode: config.appendPeriode,
      moisPrecedent: config.moisPrecedent
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) return;

    const dto: ChargeFixeConfigRequest = {
      banqueId:      this.form.value.banqueId,
      libelle:       this.form.value.libelle,
      montant:       this.form.value.montant,
      type:          this.form.value.type,
      motif:         this.isImpotTaxe ? this.form.value.motif : null,
      jourEmission:  this.formulaireVersJourEmission(),
      appendPeriode: this.form.value.appendPeriode,
      moisPrecedent: this.form.value.moisPrecedent
    };

    const obs = this.editingId
      ? this.svc.update(this.editingId, dto)
      : this.svc.create(dto);

    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Enregistré', detail: 'Charge fixe sauvegardée.' });
        this.dialogVisible = false;
        this.reloadCurrentPage();
      }
    });
  }

  confirmDelete(config: ChargeFixeConfigResponse, event: Event): void {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Supprimer la charge <b>${config.libelle}</b> (${config.banqueNom}) ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.svc.delete(config.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Supprimé', detail: 'Charge fixe supprimée.' });
          this.reloadCurrentPage();
        }
      })
    });
  }

  /** Recharge la page actuellement affichée (après create/update/delete). */
  private reloadCurrentPage(): void {
    if (this.dt) {
      this.load(this.dt.createLazyLoadMetadata());
    } else {
      this.load();
    }
  }

  typeLabel(t: TypeChargeFixe): string {
    return TypeChargeFixeLabels[t] ?? t;
  }

  motifLabel(m?: Motif | null): string {
    return m ? (MotifLabels[m] ?? m) : '—';
  }

  typeSeverity(t: TypeChargeFixe): 'info' | 'warning' | 'success' | 'danger' {
    switch (t) {
      case TypeChargeFixe.FRAIS_BANCAIRE: return 'info';
      case TypeChargeFixe.IMPOT_TAXE:     return 'warning';
      case TypeChargeFixe.AGIOS:          return 'danger';
      case TypeChargeFixe.PRELEVEMENT:    return 'success';
    }
  }

  /** Libellé lisible de la périodicité d'émission, pour l'affichage en table. */
  emissionLabel(config: ChargeFixeConfigResponse): string {
    if (config.jourEmission === -1) return 'Fin de mois';
    if (config.jourEmission === -2) return 'Fin de trimestre';
    return `Le ${config.jourEmission} du mois`;
  }
}
