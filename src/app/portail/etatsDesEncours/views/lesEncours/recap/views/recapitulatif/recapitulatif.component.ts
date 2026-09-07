import { Component, inject, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { Table } from 'primeng/table';
import { MessageService } from 'primeng/api';
import { Subscription } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RecapService } from "../../services/recap.service";
import { RecaputilatifItem } from "../../dtos/RecaputilatifItem";
import { TransactionSource, TransactionSourceLabels } from "../../../../../../transactions/dtos/TransactionSource";
import { SourceBadgeComponent } from "../../../../../../global/shared-components/source-badge/source-badge.component";
import { SensOperation } from "../../../../../../transactions/dtos/SensOperation";
import { RecapitulatifSearchDto } from "../../dtos/RecapitulatifSearchDto";
import { AgregatedRecapDto } from "../../dtos/AgregatedRecapDto";

/** Options du dropdown de filtre de sens — `null` = « Tous », aucun filtre appliqué. */
interface SensOption {
  label: string;
  value: SensOperation | null;
}

@Component({
  selector: 'app-recapitulatif',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    TooltipModule,
    ToastModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MultiSelectModule,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    SourceBadgeComponent,
  ],
  templateUrl: './recapitulatif.component.html',
  styleUrl: './recapitulatif.component.scss',
  providers: [MessageService]
})
export class RecapitulatifComponent implements OnInit, OnDestroy {
  // Table data state properties
  totalDebit!: number;
  totalCredit!: number;
  recapItems: RecaputilatifItem[] = [];
  totalRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  banqueIdFromRoute!: number;

  searchForm!: FormGroup;
  transactionSourceOptions: any[] = [];
  readonly sensOptions: SensOption[] = [
    { label: 'Tous', value: null },
    { label: 'Encaissement', value: SensOperation.ENCAISSEMENT },
    { label: 'Décaissement', value: SensOperation.DECAISSEMENT },
  ];
  readonly SensOperation = SensOperation;

  /** Explication du numéro de pièce — toutes les sources n'en portent pas. */
  readonly numeroHelp =
    'Numéro de la pièce chez sa source : chèque, LCN, virement, mise à disposition, '
    + 'impayé, numéro de marché (préfinancement, factoring, droit constaté, décompte) '
    + 'ou numéro de contrat (leasing). Les autres sources n\'en ont pas.';

  /** Explication de « date de référence » — partagée par les deux bornes du filtre. */
  readonly dateReferenceHelp =
    'Chaque opération est filtrée sur sa propre date de référence : la date d\'échéance '
    + 'pour le leasing, le CMT et les LCN, la date d\'émission pour toutes les autres sources.';

  /** Barre de filtres repliee par defaut : le tableau prend toute la hauteur au chargement. */
  showFilters = false;

  /** Panneau des bornes de date, replie par defaut pour laisser la place au tableau. */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  private readonly recapService = inject(RecapService);
  private readonly messageService = inject(MessageService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  ngOnInit(): void {
    this.initSearchForm();
    this.populateTransactionSourceOptions();

    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          if (this.dt) {
            this.dt.first = 0;
            this.loadRecapitulatif(this.dt.createLazyLoadMetadata());
          }
        }
      })
    );
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      transactionSources: [[]],
      libelle: [''],
      observation: [''],
      numero: [''],
      sens: [null],
      dateReferenceDepuis: [null],
      dateReferenceJusqua: [null],
      montant: [null]
    });

    this.subscriptions.push(
      this.searchForm.valueChanges
        .pipe(debounceTime(300), distinctUntilChanged())
        .subscribe(() => this.onFilterChange())
    );
  }

  populateTransactionSourceOptions(): void {
    this.transactionSourceOptions = Object.values(TransactionSource)
      .map(source => ({
        label: TransactionSourceLabels[source] ?? source,
        value: source
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'fr'));
  }

  onFilterChange(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadRecapitulatif(this.dt.createLazyLoadMetadata());
    }
  }

  loadRecapitulatif(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute) {
      return;
    }

    this.loading = true;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateReference,desc';

    const searchDto = this.buildSearchDto();

    this.subscriptions.push(
      this.recapService.getRecapitulatif(this.banqueIdFromRoute, searchDto, page, size, sort)
        .subscribe({
          next: (response: AgregatedRecapDto) => {
            this.recapItems = response.page.content;
            this.totalRecords = response.page.page.totalElements;
            this.loading = false;
            this.totalCredit = response.totalCredit;
            this.totalDebit = response.totalDebit;
          },
          error: (error) => {
            this.loading = false;
          }
        })
    );
  }

  exportPdf(): void {
    if (!this.banqueIdFromRoute) {
      return;
    }
    this.exporting = true;
    const searchDto = this.buildSearchDto();

    this.recapService.exportRecapitulatifPdf(this.banqueIdFromRoute, searchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recapitulatif_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  /** Remet tous les critères à leur valeur neutre et recharge la première page. */
  clearSearch(): void {
    this.searchForm.reset({
      transactionSources: [],
      libelle: '',
      observation: '',
      numero: '',
      sens: null,
      dateReferenceDepuis: null,
      dateReferenceJusqua: null,
      montant: null
    });
  }

  /**
   * Vrai dès qu'un critère est renseigné — pilote l'état du bouton « Effacer » et
   * le message affiché quand le tableau est vide (aucun résultat vs aucune donnée).
   */
  hasActiveFilters(): boolean {
    if (!this.searchForm) {
      return false;
    }
    const value = this.searchForm.value;
    return !!(value.libelle || value.observation || value.numero || value.sens || value.montant
      || value.dateReferenceDepuis || value.dateReferenceJusqua
      || (value.transactionSources && value.transactionSources.length > 0));
  }

  /** Ouvre / referme la barre de filtres. */
  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  /**
   * Résumé des critères actifs, affiché en pastilles quand la barre est repliée —
   * pour qu'un filtre en vigueur reste visible sans avoir à déplier.
   */
  get activeFilterChips(): string[] {
    if (!this.searchForm) {
      return [];
    }
    const value = this.searchForm.value;
    const chips: string[] = [];

    if (value.sens) {
      const option = this.sensOptions.find(o => o.value === value.sens);
      chips.push('Sens : ' + (option ? option.label : value.sens));
    }
    if (value.transactionSources && value.transactionSources.length > 0) {
      chips.push(value.transactionSources.length === 1
        ? 'Type : ' + (TransactionSourceLabels[value.transactionSources[0] as TransactionSource]
                       ?? value.transactionSources[0])
        : 'Types : ' + value.transactionSources.length);
    }
    if (value.libelle) {
      chips.push('Libellé : ' + value.libelle);
    }
    if (value.observation) {
      chips.push('Observation : ' + value.observation);
    }
    if (value.numero) {
      chips.push('N° : ' + value.numero);
    }
    if (value.montant !== null && value.montant !== undefined && value.montant !== '') {
      chips.push('Montant : ' + value.montant);
    }
    if (this.activeDateFilterCount > 0) {
      chips.push('Période : ' + this.activeDateFilterCount
        + (this.activeDateFilterCount > 1 ? ' bornes' : ' borne'));
    }
    return chips;
  }

  /** Nombre de critères actifs — badge du bouton « Filtres ». */
  get activeFilterCount(): number {
    return this.activeFilterChips.length;
  }

  /** Bascule le panneau des bornes de date de référence. */
  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  /** Nombre de bornes de date renseignées — affiché sur le bouton « Période ». */
  get activeDateFilterCount(): number {
    if (!this.searchForm) {
      return 0;
    }
    const value = this.searchForm.value;
    return (value.dateReferenceDepuis ? 1 : 0) + (value.dateReferenceJusqua ? 1 : 0);
  }

  /** Construit le DTO de recherche à partir du formulaire — partagé entre chargement et export. */
  private buildSearchDto(): RecapitulatifSearchDto {
    return {
      transactionSources: this.searchForm.get('transactionSources')?.value,
      libelle: this.searchForm.get('libelle')?.value,
      observation: this.searchForm.get('observation')?.value,
      numero: this.searchForm.get('numero')?.value,
      sens: this.searchForm.get('sens')?.value,
      dateReferenceDepuis: this.searchForm.get('dateReferenceDepuis')?.value,
      dateReferenceJusqua: this.searchForm.get('dateReferenceJusqua')?.value,
      montant: this.searchForm.get('montant')?.value
    };
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
