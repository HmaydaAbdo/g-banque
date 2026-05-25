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
import {FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators} from '@angular/forms';
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
import {parseLocalDate, toLocalDateString} from "../../../../global/types/date.utils";
import {TransactionUpdateRequestDto} from "../../../../transactions/dtos/TransactionUpdateRequestDto";
import {TagModule} from "primeng/tag";
import {RapprochementService} from "../../../../rapprochement/service/rapprochement-service.service";


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
    CalendarModule,
    TagModule,
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

  // Rapprochement courant (lu depuis queryParams)
  selectedRapprochementId: number | null = null;
  rapprochementStatut: 'EN_COURS' | 'CLOTURE_PROVISOIRE' | null = null;


  searchForm!: FormGroup;

  transactionSourceOptions: any[] = [];

  displayEditDialog: boolean = false;
  editDateForm!: FormGroup;

  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private rapprochementService: RapprochementService,
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
          // S'abonner aux infos banque (déjà chargées par le parent LesEncoursComponent)
          this.loadActifs();
        }
      })
    );

    this.initSearchForm();
    this.initEditDialogForm();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadActifs(): void {
    const rapprochementId = this.bankStateService.getRapprochementId();
    if (!rapprochementId) return;
    this.selectedRapprochementId = rapprochementId;

    this.rapprochementService.getActifs(this.banqueIdFromRoute).subscribe({
      next: (actifs) => {
        if (this.selectedRapprochementId === actifs.enCoursId) {
          this.rapprochementStatut = 'EN_COURS';
        } else if (this.selectedRapprochementId === actifs.provisoireId) {
          this.rapprochementStatut = 'CLOTURE_PROVISOIRE';
        }
        this.onSearch();
      },
      error: () => {
        this.onSearch();
      }
    });
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
      montant: [null],
      dateOperationDebut: [null],
      dateOperationFin: [null]

    });



    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    this.subscriptions.push(formValueChangesSub);
  }
  initEditDialogForm(): void {
    this.editDateForm = this.fb.group({
      id: [null],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      tvaDeductible: [null, Validators.required],
      tvaCollecte: [null, Validators.required]
    });
  }



  loadTransactions(event: TableLazyLoadEvent): void {
    if (!this.banqueIdFromRoute || !this.selectedRapprochementId) {
      return;
    }

    this.loading = true;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = event.sortField ? `${event.sortField},${sortOrder}` : 'createdAt,desc';

    const searchDto: TransactionSearchDto = {
      ...(this.searchForm.value as TransactionSearchDto),
      rapprochementId: this.selectedRapprochementId
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

  openEditDialog(transaction: TransactionResponseDto): void {
    this.editDateForm.patchValue({
      id: transaction.id,
      dateValeur: parseLocalDate(transaction.dateValeur),
      dateOperation: parseLocalDate(transaction.dateOperations),
      tvaDeductible: transaction.tvaDeductible,
      tvaCollecte: transaction.tvaCollectee
    });
    this.displayEditDialog = true;
  }

  updateTransaction(): void {
    if (!this.editDateForm.valid) return;

    const formValue = this.editDateForm.value;
    const updateDto: TransactionUpdateRequestDto = {
      dateValeur: toLocalDateString(formValue.dateValeur)!,
      dateOperation: toLocalDateString(formValue.dateOperation)!,
      tvaDeductible: formValue.tvaDeductible,
      tvaCollecte: formValue.tvaCollecte
    };

    // Ici, utilise l'id stocké dans le formulaire ou passé en paramètre
    const transactionId = formValue.id; // si tu l’ajoutes au FormGroup
    this.transactionService.updateTransactionById(transactionId, updateDto)
      .subscribe({
        next: (updatedTransaction) => {
          const index = this.transactions.findIndex(t => t.id === updatedTransaction.id);
          if (index !== -1) {
            this.transactions[index] = updatedTransaction;
          }
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Transaction mise à jour avec succès.'
          });
          this.closeEditDialog();
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: `Impossible de mettre à jour la transaction : ${err.error?.message || 'Erreur inconnue'}`
          });
        }
      });
  }
  closeEditDialog(): void {
    this.displayEditDialog = false;
    this.editDateForm.reset()
  }




}
