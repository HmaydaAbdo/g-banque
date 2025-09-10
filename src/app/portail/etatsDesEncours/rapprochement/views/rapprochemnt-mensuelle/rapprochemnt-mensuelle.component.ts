import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { TransactionResponseDto } from '../../dtos/transactions/TransactionResponseDto';
import { CommonModule, DatePipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { PaginatorModule } from 'primeng/paginator';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ActivatedRoute } from '@angular/router';
import { PageResponse } from "../../../../global/types/PageResponse";
import { TransactionSearchDto } from '../../dtos/transactions/TransactionSearchDto';
import {TransactionService} from "../../services/transactions/TransactionService"; // Import du DTO de recherche

@Component({
  selector: 'app-rapprochemnt-mensuelle',
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
    DatePipe
  ],
  templateUrl: './rapprochemnt-mensuelle.component.html',
  styleUrl: './rapprochemnt-mensuelle.component.scss',
  providers: [MessageService]
})
export class RapprochemntMensuelleComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt!: Table;

  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  libelleControl!: FormControl;

  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private datePipe: DatePipe
  ) { }

  ngOnInit(): void {
    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          this.initSearchForm();
          // Le lazy loading se chargera de la première requête
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: ['']
    });

    // Assigner le FormControl à la propriété pour une utilisation sûre
    this.libelleControl = this.searchForm.get('libelle') as FormControl;

    const formValueChangesSub = this.libelleControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.onSearch();
      });

    this.subscriptions.push(formValueChangesSub);
  }

  loadTransactions(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute) {
      console.error("L'ID de la banque n'a pas été trouvé dans la route.");
      return;
    }

    this.loading = true;
    const libelle = this.libelleControl.value;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'createdAt,desc';

    const searchDto: TransactionSearchDto = { libelle: libelle };

    // Changement ici : appel du nouveau service avec le DTO de recherche
    this.transactionService.getTransactionsByBanque(this.banqueIdFromRoute, searchDto, page, size, sort)
      .subscribe({
        next: (response: PageResponse<TransactionResponseDto>) => {
          this.transactions = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des transactions:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des transactions.'
          });
        }
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadTransactions(this.dt.createLazyLoadMetadata());
    }
  }
}
