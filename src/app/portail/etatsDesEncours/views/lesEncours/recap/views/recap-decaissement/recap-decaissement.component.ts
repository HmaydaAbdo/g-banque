import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
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
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RecapService } from "../../services/recap.service";
import { PageResponse } from "../../../../../../global/types/PageResponse";
import { RecaputilatifItem } from "../../dtos/RecaputilatifItem";
import { TransactionSource } from "../../../../../rapprochement/dtos/transactions/TransactionSource";
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
  banqueIdFromRoute!: number;

  searchForm!: FormGroup;
  transactionSourceOptions: any[] = [];

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private recapitulatifDecaissementService: RecapService,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private fb: FormBuilder
  ) { }

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
      observation: ['']
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
      observation: this.searchForm.get('observation')?.value
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
            console.error('Error fetching disbursements:', error);
            this.loading = false;
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec du chargement des décaissements.'
            });
          }
        })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
