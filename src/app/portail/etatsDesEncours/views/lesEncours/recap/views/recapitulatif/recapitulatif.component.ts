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
import { TransactionSource } from "../../../../../../transactions/dtos/TransactionSource";
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
    this.transactionSourceOptions = Object.values(TransactionSource).map(source => ({
      label: source,
      value: source
    }));
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
      : 'dateEmission,desc';

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

  /** Construit le DTO de recherche à partir du formulaire — partagé entre chargement et export. */
  private buildSearchDto(): RecapitulatifSearchDto {
    return {
      transactionSources: this.searchForm.get('transactionSources')?.value,
      libelle: this.searchForm.get('libelle')?.value,
      observation: this.searchForm.get('observation')?.value,
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
