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
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { debounceTime } from 'rxjs/operators';

import { CommissionConfigService } from '../../service/commission-config.service';
import { CommissionConfigResponse } from '../../dtos/CommissionConfigResponse';
import { CommissionConfigRequest } from '../../dtos/CommissionConfigRequest';
import { CommissionCondition, CommissionConditionLabels } from '../../dtos/CommissionCondition';
import { TransactionSource, TransactionSourceLabels } from '../../../../transactions/dtos/TransactionSource';
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {OrganismeLeasingService} from "../../../../leasing/organismesLeasing/services/organisme-leasing.service";
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {OrganismeLeasingResponse} from "../../../../leasing/organismesLeasing/dtos/OrganismeLeasingResponse";
import {PageResponse} from "../../../../global/types/PageResponse";

@Component({
  selector: 'app-commission-config-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    TableModule, ButtonModule, RippleModule, DialogModule,
    InputNumberModule, InputTextModule, DropdownModule,
    ToastModule, ConfirmDialogModule, TooltipModule, BreadcrumbModule, TagModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './commission-config-list.component.html'
})
export class CommissionConfigListComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  private svc                   = inject(CommissionConfigService);
  private banqueService         = inject(BanqueService);
  private organismeLeasingService = inject(OrganismeLeasingService);
  private messageService        = inject(MessageService);
  private confirmationService   = inject(ConfirmationService);
  private fb                    = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home',      route: '/portail/accueil',                  label: 'Accueil' },
    { icon: 'pi pi-shield',    label: 'Administration' },
    { icon: 'pi pi-sliders-h', route: '/portail/admin/commission-config',  label: 'Commissions config.' }
  ];

  configs: CommissionConfigResponse[] = [];
  banques: BanqueResponse[]              = [];
  organismes: OrganismeLeasingResponse[] = [];

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

  typeOptions = Object.values(TransactionSource).map(v => ({
    label: TransactionSourceLabels[v], value: v
  }));

  conditionOptions = Object.values(CommissionCondition).map(v => ({
    label: CommissionConditionLabels[v], value: v
  }));


  ngOnInit(): void {
    this.buildForm();
    this.buildFilterForm();

    this.banqueService.getAllBanques({}, 0, 1000).subscribe(res => this.banques = res.content);
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000).subscribe(res => this.organismes = res.content);

    // Reset du champ référence quand le type change
    // (évite de garder un organisme leasing sélectionné en mode Virement et vice-versa)
    this.form.get('type')?.valueChanges.subscribe(() => {
      this.form.get('organismeNom')?.reset(null);
    });
  }

  private buildFilterForm(): void {
    this.filterForm = this.fb.group({
      search:    [''],
      banqueId:  [null],
      type:      [null],
      condition: [null]
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
    this.filterForm.reset({ search: '', banqueId: null, type: null, condition: null });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      banqueId:     [null, Validators.required],
      organismeNom: [null],
      montant:      [null, [Validators.required, Validators.min(0.01)]],
      type:         [null, Validators.required],
      condition:    [CommissionCondition.DEFAULT, Validators.required]
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

    const { search, banqueId, type, condition } = this.filterForm.value;

    this.svc.findAll({ search, banqueId, type, condition }, page, rows as number, sort).subscribe({
      next: (res: PageResponse<CommissionConfigResponse>) => {
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

  get organismeOptions() {
    return [
      { label: '— Aucun —', value: null },
      ...this.organismes.map(o => ({ label: o.nom, value: o.nom }))
    ];
  }

  /**
   * Options du champ "référence" (organismeNom en base) :
   * - Virement → liste des banques (valeur = nom, pour correspondre à banqueBeneficiaire.nom)
   * - Autres types → liste des organismes leasing
   */
  get referenceOptions() {
    const currentType = this.form.get('type')?.value as string | null;
    if (currentType === 'Virement') {
      return [
        { label: '— Aucune —', value: null },
        ...this.banques.map(b => ({ label: b.nom, value: b.nom }))
      ];
    }
    return this.organismeOptions;
  }

  get referenceLabel(): string {
    const currentType = this.form.get('type')?.value as string | null;
    return currentType === 'Virement'
      ? 'Banque bénéficiaire'
      : 'Organisme leasing';
  }

  get referencePlaceholder(): string {
    const currentType = this.form.get('type')?.value as string | null;
    return currentType === 'Virement' ? '— Toutes banques —' : '— Aucun —';
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({ condition: CommissionCondition.DEFAULT, organismeNom: null });
    this.dialogVisible = true;
  }

  openEdit(config: CommissionConfigResponse): void {
    this.editingId = config.id;
    this.form.patchValue({
      banqueId:     config.banqueId,
      organismeNom: config.organismeNom ?? null,
      montant:      config.montant,
      type:         config.type,
      condition:    config.condition
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid) return;

    const dto: CommissionConfigRequest = {
      banqueId:     this.form.value.banqueId,
      organismeNom: this.form.value.organismeNom || null,
      montant:      this.form.value.montant,
      type:         this.form.value.type,
      condition:    this.form.value.condition
    };

    const obs = this.editingId
      ? this.svc.update(this.editingId, dto)
      : this.svc.create(dto);

    obs.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Enregistré', detail: 'Règle sauvegardée.' });
        this.dialogVisible = false;
        this.reloadCurrentPage();
      }
    });
  }

  confirmDelete(config: CommissionConfigResponse, event: Event): void {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Supprimer la règle pour <b>${config.banqueNom}</b> ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.svc.delete(config.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Supprimé', detail: 'Règle supprimée.' });
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

  typeLabel(t: TransactionSource): string {
    return TransactionSourceLabels[t] ?? t;
  }

  conditionLabel(c: CommissionCondition): string {
    return CommissionConditionLabels[c] ?? c;
  }

  conditionSeverity(c: CommissionCondition): 'success' | 'info' | 'warning' {
    if (c === CommissionCondition.DEFAULT)  return 'info';
    if (c === CommissionCondition.SI_MATCH) return 'success';
    return 'warning';
  }
}
