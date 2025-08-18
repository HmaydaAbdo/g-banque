import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
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
import {TransactionResponseDto} from "../rapprochement/dtos/TransactionResponseDto";
import {RapprochemntService} from "../rapprochement/services/rapprochemnt.service";
import {PageResponse} from "../../global/types/PageResponse";


@Component({
  selector: 'app-recap-rapprochement',
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
  templateUrl: './recap-rapprochement.component.html',
  styleUrl: './recap-rapprochement.component.scss',
  providers: [MessageService]
})
export class RecapRapprochementComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt!: Table;

  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  libelleControl!: FormControl;

  private subscriptions: Subscription[] = [];

  constructor(
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private datePipe: DatePipe
  ) { }

  ngOnInit(): void {
    this.initSearchForm();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: ['']
    });

    this.libelleControl = this.searchForm.get('libelle') as FormControl;

    const formValueChangesSub = this.libelleControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.onSearch();
      });

    this.subscriptions.push(formValueChangesSub);
  }

  loadRecap(event: TableLazyLoadEvent): void {
    this.loading = true;
    const libelle = this.libelleControl.value;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField && event.sortOrder
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'createdAt,desc';

    this.rapprochementService.getRecapitulatifBancaire(libelle, page, size, sort)
      .subscribe({
        next: (response: PageResponse<TransactionResponseDto>) => {
          this.transactions = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement du récapitulatif:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement du récapitulatif bancaire.'
          });
        }
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadRecap(this.dt.createLazyLoadMetadata());
    }
  }
}
