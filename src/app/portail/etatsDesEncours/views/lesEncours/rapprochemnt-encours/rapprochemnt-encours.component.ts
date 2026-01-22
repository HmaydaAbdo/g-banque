import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { TransactionResponseDto } from '../../../../transactions/dtos/TransactionResponseDto';
import { CommonModule, DatePipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { PaginatorModule } from 'primeng/paginator';
import { InputTextModule } from 'primeng/inputtext';
import {FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormControl} from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {ConfirmationService, MessageService} from 'primeng/api';
import { ActivatedRoute } from '@angular/router';
import { TransactionSearchDto } from '../../../../transactions/dtos/TransactionSearchDto';
import { TransactionService } from "../../../../transactions/service/TransactionService";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {MultiSelectModule} from "primeng/multiselect";
import {TransactionSource} from "../../../../transactions/dtos/TransactionSource";
import {AgreratedTransactionResponseDto} from "../../../../transactions/dtos/AgreratedTransactionResponseDto";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {ValidationService} from "../../../services/validation/validation.service";
import {BankStateService} from "../../../../comptes-bancaire/services/bank-state.service";


@Component({
  selector: 'app-rapprochemnt-encours',
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
    ConfirmDialogModule,
    BreadcrumbModule,
    MultiSelectModule,
    DialogModule,
    CalendarModule
  ],
  templateUrl: './rapprochemnt-encours.component.html',
  styleUrls: ['./rapprochemnt-encours.component.scss'],
  providers: [MessageService,ConfirmationService]
})
export class RapprochemntEncoursComponent implements OnInit, OnDestroy {
  @ViewChild('dt') dt!: Table;

  agregatedData: AgreratedTransactionResponseDto | null = null;
  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  libelleControl!: FormControl;
  observationControl!: FormControl;
  transactionSourcesControl!: FormControl;

  transactionSourceOptions: any[] = [];


  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private fb: FormBuilder,
    private validationService: ValidationService,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private datePipe: DatePipe,
    private bankStateService: BankStateService
  ) {
  }

  ngOnInit(): void {
    this.populateTransactionSourceOptions();


    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          this.initSearchForm();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  // Populates the options for the multi-select from the TransactionSource enum.
  populateTransactionSourceOptions(): void {
    this.transactionSourceOptions = Object.entries(TransactionSource).map(([key, value]) => ({
      label: value,
      value: key
    }));
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: [''],
      transactionSources: [[]],
      observation:[''],
      montant: [null]
    });

    this.libelleControl = this.searchForm.get('libelle') as FormControl;
    this.transactionSourcesControl = this.searchForm.get('transactionSources') as FormControl;
    this.observationControl = this.searchForm.get('observation') as FormControl;

    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    this.subscriptions.push(formValueChangesSub);
  }


  loadTransactions(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute) {
      console.error("L'ID de la banque n'a pas été trouvé dans la route.");
      return;
    }

    this.loading = true;

    const { libelle, transactionSources ,observation,montant} = this.searchForm.value;
    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = event.sortField ? `${event.sortField},${sortOrder}` : 'createdAt,desc';

    const searchDto: TransactionSearchDto = {
      libelle: libelle,
      transactionSources: transactionSources,
      observation:observation,
      montant: montant
    };

    this.transactionService.getTransactionsByBanque(this.banqueIdFromRoute, searchDto, page, size, sort)
      .subscribe({
        next: (response: AgreratedTransactionResponseDto) => {
          this.agregatedData = response;
          this.transactions = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des dtos:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des dtos.'
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


  rollbackTransaction(transactionId: number): void {
    this.validationService.rollBackTransaction(transactionId).subscribe({
      next: () => {
        this.messageService.add({severity: 'success',summary: 'Transaction supprimé avec success'});
        this.loadTransactions(this.dt.createLazyLoadMetadata());
        this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
      },
      error: (error) => {
        this.messageService.add({severity: 'error',summary: 'Erreur lors de la suppression de la transaction .',})
      }
    });
  }


}
