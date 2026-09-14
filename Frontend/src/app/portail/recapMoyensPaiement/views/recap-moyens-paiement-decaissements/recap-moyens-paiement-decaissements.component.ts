import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { Button } from 'primeng/button';
import { DatePipe, DecimalPipe, NgClass, NgIf } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { MenuItem, SelectItem } from 'primeng/api';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Ripple } from 'primeng/ripple';
import { CalendarModule } from 'primeng/calendar';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { RecapMoyenPaiementItem } from '../../dtos/RecapMoyenPaiementItem';
import { RecapMoyenPaiementSearchDto } from '../../dtos/RecapMoyenPaiementSearchDto';
import { RecapMoyenPaiementService } from '../../services/RecapMoyenPaiementService';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import { StatutMoyenPaiement } from '../../../global/enums/StatutMoyenPaiement';
import { TransactionSource, TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';
import { SourceBadgeComponent } from '../../../global/shared-components/source-badge/source-badge.component';

/**
 * Les moyens de paiement couverts par le recap (Prélèvements et Cautions sont hors périmètre).
 * LCN est décliné en 3 sources distinctes (encaissement/décaissement/escompte), comme partout
 * ailleurs dans l'application — la valeur générique TransactionSource.LCN n'est pas utilisée en
 * pratique (donnée historique non fiable).
 */
const MOYENS_DE_PAIEMENT_SOURCES: TransactionSource[] = [
  TransactionSource.CHEQUE,
  TransactionSource.LCN_ENCAISSEMENT,
  TransactionSource.LCN_DECAISSEMENT,
  TransactionSource.LCN_ESCOMPTE,
  TransactionSource.Virement,
  TransactionSource.MiseADisposition,
  TransactionSource.VIREMENT_INTERNATIONAL,
];

@Component({
  selector: 'app-recap-moyens-paiement-decaissements',
  standalone: true,
  imports: [
    RouterLink,
    BreadcrumbModule,
    Button,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    MultiSelectModule,
    InputTextModule,
    NgIf,
    PaginatorModule,
    ReactiveFormsModule,
    TableModule,
    TagModule,
    ToastModule,
    NgClass,
    Ripple,
    CalendarModule,
    FormsModule,
    SourceBadgeComponent
  ],
  templateUrl: './recap-moyens-paiement-decaissements.component.html',
  styleUrl: './recap-moyens-paiement-decaissements.component.scss'
})
export class RecapMoyensPaiementDecaissementsComponent implements OnInit, OnDestroy {

  protected readonly StatutMoyenPaiement = StatutMoyenPaiement;

  items: RecapMoyenPaiementItem[] = [];
  totalRecords = 0;
  totalMontant = 0;
  loading = false;
  exporting = false;

  breadcrumbItems: MenuItem[] | undefined;
  searchForm!: FormGroup;

  banqueOptions: SelectItem[] = [];
  sourceOptions: SelectItem[] = MOYENS_DE_PAIEMENT_SOURCES.map(source => ({
    label: TransactionSourceLabels[source],
    value: source
  }));
  statutOptions: SelectItem[] = [
    { label: 'En cours', value: StatutMoyenPaiement.ENCOURS },
    { label: 'Réglé', value: StatutMoyenPaiement.REGLE },
    { label: 'Annulé', value: StatutMoyenPaiement.ANNULE }
  ];

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private recapService: RecapMoyenPaiementService,
    private fb: FormBuilder,
    private banqueService: BanqueService
  ) {
  }

  ngOnInit(): void {
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/recap-moyens-paiement/decaissements', label: 'Recap Décaissement — Moyens de Paiement' },
    ];
    this.initSearchForm();
    this.loadBanques();
    this.loadItems(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      sources: [[]],
      statuts: [[]],
      libelle: [''],
      numero: [''],
      montant: [null],
      dateOperationFrom: [null],
      dateOperationTo: [null],
      dateValeurFrom: [null],
      dateValeurTo: [null],
    });
    const sub = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(sub);
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateOperationFrom || v.dateOperationTo) count++;
    if (v.dateValeurFrom || v.dateValeurTo) count++;
    return count;
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(b => ({ label: b.nom, value: b.id }));
        this.banqueOptions.unshift({ label: 'Toutes les banques', value: null });
      },
      error: () => {}
    });
  }

  loadItems(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateValeur,desc';

    const searchDto: RecapMoyenPaiementSearchDto = this.searchForm.getRawValue();

    this.subscriptions.push(
      this.recapService.getDecaissements(searchDto, page, size, sort).subscribe({
        next: (response) => {
          this.items = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.totalMontant = response.total;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      })
    );
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadItems(this.dt.createLazyLoadMetadata());
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      banqueId: null, sources: [], statuts: [], libelle: '', numero: '',
      montant: null,
      dateOperationFrom: null, dateOperationTo: null,
      dateValeurFrom: null, dateValeurTo: null,
    });
    if (this.dt) {
      this.dt.first = 0;
      this.loadItems(this.dt.createLazyLoadMetadata());
    }
  }

  getStatutSeverity(statut: StatutMoyenPaiement): 'success' | 'warning' | 'danger' | 'info' {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'success';
      case StatutMoyenPaiement.REGLE:   return 'warning';
      case StatutMoyenPaiement.ANNULE:  return 'danger';
      default: return 'info';
    }
  }

  getStatutLabel(statut: StatutMoyenPaiement): string {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'En cours';
      case StatutMoyenPaiement.REGLE:   return 'Réglé';
      case StatutMoyenPaiement.ANNULE:  return 'Annulé';
      default: return statut;
    }
  }

  exportPdf(): void {
    this.exporting = true;
    const searchDto: RecapMoyenPaiementSearchDto = this.searchForm.getRawValue();
    this.recapService.exportDecaissementsPdf(searchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recap_decaissement_moyens_paiement_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }
}
