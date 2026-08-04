import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';

// PrimeNG Modules
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MenuItem, MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputSwitchModule } from 'primeng/inputswitch';
import { DialogModule } from 'primeng/dialog';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { RippleModule } from 'primeng/ripple';

import { TableauAmortissement } from '../../dtos/table-amortissement';
import { EcheanceLeasingResponse } from '../../dtos/EcheanceLeasingResponse';
import { EcheanceLeasingUpdateRequest } from '../../dtos/EcheanceLeasingUpdateRequest';
import { StatutEcheanceLeasing } from '../../dtos/StatutEcheanceLeasing';
import { EcheanceLeasingService } from '../../services/EcheanceLeasingService';
import { EcheanceLeasingSearchDTO } from '../../dtos/echeance-leasing-search';
import { AuthService } from '../../../../../auth/services/auth.service';
import { CalendarModule } from 'primeng/calendar';


@Component({
  selector: 'app-tableau-amortissement',
  templateUrl: './tableau-amortissement.component.html',
  styleUrls: ['./tableau-amortissement.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    InputTextModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ProgressSpinnerModule,
    DividerModule,
    InputNumberModule,
    InputSwitchModule,
    FormsModule,
    DialogModule,
    InputTextareaModule,
    BreadcrumbModule,
    CalendarModule,
    RippleModule
  ],
  providers: [MessageService]
})
export class TableauAmortissementComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly echeanceLeasingService = inject(EcheanceLeasingService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  contratId!: number;
  tableauAmortissement: TableauAmortissement | null = null;
  echeances: EcheanceLeasingResponse[] = [];
  totalEcheancesRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  errorMessage: string | null = null;

  searchForm!: FormGroup;
  statutLeasingOptions = Object.values(StatutEcheanceLeasing).map(status => ({ label: status, value: status }));

  showEditDialog = false;
  editingEcheanceId: number | null = null;
  editForm!: FormGroup;

  private subscriptions: Subscription[] = [];

  protected lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 3,
    sortField: 'id',
    sortOrder: 1,
    filters: {}
  };
  items: MenuItem[] | undefined;

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/leasing', label: 'Les Contracts Leasing' },
      { icon: 'pi pi-table', route: '/portail/leasing', label: "Tableau d'amortissement" },
    ];
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.contratId = +id;
        this.initSearchForm();
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
      } else {
        this.errorMessage = 'Contract ID not provided in the URL.';
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroEcheance: [null],
      dateEcheanceDebut: [null as Date | null],
      dateEcheanceFin: [null as Date | null],
      dateOperationDebut: [null as Date | null],
      dateOperationFin: [null as Date | null],
      dateValeurDebut: [null as Date | null],
      dateValeurFin: [null as Date | null],
      statutPaiement: [null]
    });

    this.editForm = this.fb.group({
      dateEcheance: [null],
      montantTTC: [null],
      dateOperation: [null],
      dateValeur: [null],
      observation: [null],
      libelle: [null]
    });

    const searchSub = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.lastLazyLoadEvent.first = 0;
      this.loadTableauAmortissement(this.lastLazyLoadEvent);
    });
    this.subscriptions.push(searchSub);
  }

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEcheanceDebut || v.dateEcheanceFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
  }

  loadTableauAmortissement(event: TableLazyLoadEvent): void {
    if (!this.contratId) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sortBy: string | undefined;
    let sortDirection: 'asc' | 'desc' | undefined;

    if (event.sortField) {
      sortBy = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    } else {
      sortBy = this.lastLazyLoadEvent.sortField as string;
      sortDirection = this.lastLazyLoadEvent.sortOrder === 1 ? 'asc' : 'desc';
    }

    const searchCriteria: EcheanceLeasingSearchDTO = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
      statutPaiement: this.searchForm.get('statutPaiement')?.value
    };

    this.echeanceLeasingService.getTableauAmortissementForContrat(
      this.contratId,
      searchCriteria,
      currentPage,
      pageSize,
      sortBy,
      sortDirection
    ).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (data: TableauAmortissement) => {
        this.tableauAmortissement = data;
        this.echeances = data.echeances.content;
        this.totalEcheancesRecords = data.echeances.page.totalElements;
      },
      error: () => {
        this.errorMessage = 'Échec du chargement du tableau d\'amortissement. Veuillez réessayer.';
      }
    });
  }

  exportPdf(): void {
    if (!this.contratId) return;
    this.exporting = true;

    const searchCriteria: EcheanceLeasingSearchDTO = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
      statutPaiement: this.searchForm.get('statutPaiement')?.value
    };

    this.echeanceLeasingService.exportTableauAmortissementPdf(this.contratId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `tableau_amortissement_leasing_${this.contratId}_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      }
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      numeroEcheance: null,
      dateEcheanceDebut: null,
      dateEcheanceFin: null,
      dateOperationDebut: null,
      dateOperationFin: null,
      dateValeurDebut: null,
      dateValeurFin: null,
      statutPaiement: null
    });
  }

  getStatutSeverity(status: StatutEcheanceLeasing): 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' | undefined {
    switch (status) {
      case StatutEcheanceLeasing.PAYE:
        return 'success';
      case StatutEcheanceLeasing.PREVU:
        return 'info';
      default:
        return 'warning';
    }
  }

  onStatutChange(echeance: EcheanceLeasingResponse): void {
    this.loading = true;
    this.echeanceLeasingService.toggleStatus(echeance.id).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: () => {
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
      },
      error: () => {
        this.errorMessage = 'Échec de la mise à jour du statut. Veuillez réessayer.';
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  openEditDialog(echeance: EcheanceLeasingResponse): void {
    this.editingEcheanceId = echeance.id;
    this.editForm.reset({
      dateEcheance: echeance.dateEcheance ? new Date(echeance.dateEcheance) : null,
      montantTTC: echeance.montantTTC,
      dateOperation: echeance.dateOperation ? new Date(echeance.dateOperation) : null,
      dateValeur: echeance.dateValeur ? new Date(echeance.dateValeur) : null,
      observation: echeance.observation ?? null,
      libelle: echeance.libelle ?? null
    });
    this.showEditDialog = true;
  }

  closeEditDialog(): void {
    this.showEditDialog = false;
    this.editingEcheanceId = null;
    this.editForm.reset();
  }

  saveEdit(): void {
    if (!this.editingEcheanceId || this.editForm.invalid) return;

    const toIso = (d: Date | null | undefined): string | null =>
      d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : null;

    const raw = this.editForm.value;
    const dto: EcheanceLeasingUpdateRequest = {
      dateEcheance: toIso(raw.dateEcheance)!,
      montantTTC: raw.montantTTC,
      dateOperation: toIso(raw.dateOperation),
      dateValeur: toIso(raw.dateValeur),
      observation: raw.observation ?? null,
      libelle: raw.libelle ?? null
    };

    this.loading = true;
    this.echeanceLeasingService.updateEcheance(this.editingEcheanceId, dto).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: () => {
        this.closeEditDialog();
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Échéance mise à jour avec succès.'
        });
      }
    });
  }
}
