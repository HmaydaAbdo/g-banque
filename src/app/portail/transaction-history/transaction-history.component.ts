import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { PaginatorModule } from 'primeng/paginator';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {MenuItem, MessageService} from 'primeng/api';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { TagModule } from 'primeng/tag';

// Import your DTOs and services
import { TransactionResponseDto } from "../etatsDesEncours/rapprochement/dtos/TransactionResponseDto";
import { BanqueResponse } from "../comptes-bancaire/dtos/BanqueResponse";
import { TransactionService } from "../etatsDesEncours/rapprochement/services/transactions/TransactionService";
import { BanqueService } from "../comptes-bancaire/services/BanqueService";
import { TransactionSource } from "../etatsDesEncours/rapprochement/dtos/TransactionSource";
import { TransactionSearchDto } from "../etatsDesEncours/rapprochement/dtos/TransactionSearchDto";
import { PageResponse } from "../global/types/PageResponse";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive} from "@angular/router";


@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    PaginatorModule,
    InputTextModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    DropdownModule,
    InputGroupModule,
    InputGroupAddonModule,
    TagModule,
    BreadcrumbModule,
    RouterLinkActive
  ],
  templateUrl: './transaction-history.component.html',
  styleUrl: './transaction-history.component.scss',
  providers: [MessageService]
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt!: Table;

  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;

  searchForm!: FormGroup;
  transactionSources: string[] = [];
  banquesOptions: { label: string, value: number | null }[] = [];

  items: MenuItem[] | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private banqueService: BanqueService,
    private fb: FormBuilder,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.initSearchForm();
    this.loadBanques();
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/historique-des-transactions', label:"Historique des transactions" },
    ]
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the reactive form and sets up a single subscription for all filter changes.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: [''],
      banqueId: [null],
      transactionSource: [null]
    });

    this.transactionSources = Object.values(TransactionSource);

    // Consolidate all filter subscriptions into a single one
    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        // Use a more robust distinctUntilChanged check for form objects
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onSearch();
      });

    this.subscriptions.push(formValueChangesSub);
  }

  /**
   * Loads the list of banks for the filter dropdown.
   */
  loadBanques(): void {
    this.subscriptions.push(
      this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
        .subscribe({
          next: (pageResponse: PageResponse<BanqueResponse>) => {
            this.banquesOptions = [
              { label: 'Toutes les banques', value: null },
              ...pageResponse.content.map(banque => ({ label: banque.nom, value: banque.id! }))
            ];
          },
          error: (err) => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques pour le filtre.', life: 3000 });
            console.error('Error loading banks for filter:', err);
          }
        })
    );
  }

  /**
   * This method is the single source of truth for loading data from the backend.
   * It's called by the p-table's (onLazyLoad) event.
   */
  loadTransactions(event: TableLazyLoadEvent): void {
    this.loading = true;

    // Get the current search criteria from the reactive form
    const searchDto: TransactionSearchDto = this.searchForm.value;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.subscriptions.push(
      this.transactionService.getTransactionHistory(searchDto, page, size, sort)
        .subscribe({
          next: (response: PageResponse<TransactionResponseDto>) => {
            this.transactions = response.content;
            this.totalRecords = response.page.totalElements;
            this.loading = false;
          },
          error: (error) => {
            this.loading = false;
            console.error('Erreur lors du chargement des transactions:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec du chargement de l\'historique des transactions.'
            });
          }
        })
    );
  }

  /**
   * Triggers a new search by resetting the table's state and manually
   * calling the data loading method with the new metadata.
   */
  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      // Get the metadata from the table and explicitly call the load method
      this.loadTransactions(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Clears all filter criteria and triggers a new search.
   */
  clearSearch(): void {
    this.searchForm.reset({
      libelle: '',
      banqueId: null,
      transactionSource: null
    });
    // This will trigger the form's valueChanges subscription, which in turn calls onSearch().
    // This keeps the logic clean and centralized.
  }
}
