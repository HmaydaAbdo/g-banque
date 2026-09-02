import { Component, inject, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { PaginatorModule } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { Table } from 'primeng/table';
import { MessageService } from 'primeng/api';
import { Subscription } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext'; // Ajouté pour le champ de recherche
import { CalendarModule } from 'primeng/calendar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RecapService } from "../../services/recap.service";
import { PageResponse } from "../../../../../../global/types/PageResponse";
import { RecaputilatifItem } from "../../dtos/RecaputilatifItem";
import { TransactionSource } from "../../../../../../transactions/dtos/TransactionSource";
import { DecaissementSearchDto } from "../../dtos/DecaissementSearchDto";
import {AgregatedRecapDto} from "../../dtos/AgregatedRecapDto";
import {Decimal} from "decimal.js";


@Component({
  selector: 'app-recap-decaissement',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    PaginatorModule,
    TooltipModule,
    ToastModule,
    DatePipe,
    DecimalPipe,
    FormsModule,
    ReactiveFormsModule,
    ToolbarModule,
    MultiSelectModule,
    InputTextModule,
    CalendarModule,
    ButtonModule,
    RippleModule,
  ],
  templateUrl: './recap-decaissement.component.html',
  styleUrl: './recap-decaissement.component.scss',
  providers: [MessageService]
})
export class RecapDecaissementComponent implements OnInit, OnDestroy {
  // Table data state properties
  totalDebit!:number
  totalCredit!:number
  recapDecaissements: RecaputilatifItem[] = [];
  totalRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  banqueIdFromRoute!: number;

  searchForm!: FormGroup;
  transactionSourceOptions: any[] = [];

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  private readonly recapitulatifDecaissementService = inject(RecapService);
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
            this.loadDecaissements(this.dt.createLazyLoadMetadata());
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
      dateReferenceJusqua: [null]
    });

    this.subscriptions.push(
      this.searchForm.valueChanges
        .pipe(debounceTime(300), distinctUntilChanged())
        .subscribe(() => this.onFilterChange())
    );
  }

  populateTransactionSourceOptions(): void {
    this.transactionSourceOptions = Object.entries(TransactionSource).map(([key, value]) => ({
      label: value,
      value: key
    }));
  }
  onFilterChange(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadDecaissements(this.dt.createLazyLoadMetadata());
    }
  }

  loadDecaissements(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute) {
      return;
    }

    this.loading = true;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,asc';

    const searchDto: DecaissementSearchDto = {
      transactionSources: this.searchForm.get('transactionSources')?.value,
      libelle: this.searchForm.get('libelle')?.value,
      observation: this.searchForm.get('observation')?.value,
      dateReferenceJusqua: this.searchForm.get('dateReferenceJusqua')?.value
    };

    this.subscriptions.push(
      this.recapitulatifDecaissementService.getRecapitulatifDecaissement(this.banqueIdFromRoute, searchDto, page, size, sort)
        .subscribe({
          next: (response: AgregatedRecapDto) => {
            this.recapDecaissements = response.page.content;
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
    const searchDto: DecaissementSearchDto = {
      transactionSources: this.searchForm.get('transactionSources')?.value,
      libelle: this.searchForm.get('libelle')?.value,
      observation: this.searchForm.get('observation')?.value,
      dateReferenceJusqua: this.searchForm.get('dateReferenceJusqua')?.value
    };

    this.recapitulatifDecaissementService.exportRecapitulatifDecaissementPdf(this.banqueIdFromRoute, searchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `recapitulatif_decaissement_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
