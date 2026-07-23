import { Component, OnInit, ViewChild, OnDestroy, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { SourceBadgeComponent } from '../../../global/shared-components/source-badge/source-badge.component';
import { ActivatedRoute } from '@angular/router';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectButtonModule } from 'primeng/selectbutton';
import { Subscription, catchError, debounceTime, distinctUntilChanged, finalize, throwError } from 'rxjs';
import { RapprochementResponse } from '../../dtos/RapprochementResponse';
import { TransactionResponseDto } from '../../../transactions/dtos/TransactionResponseDto';
import { TransactionSearchDto } from '../../../transactions/dtos/TransactionSearchDto';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { RapprochementService } from "../../service/rapprochement-service.service";
import {DialogModule} from "primeng/dialog";
import {TransactionUpdateRequestDto} from "../../../transactions/dtos/TransactionUpdateRequestDto";
import {TransactionService} from "../../../transactions/service/TransactionService";
import {Ripple} from "primeng/ripple";
import { parseLocalDate, toLocalDateString } from '../../../global/types/date.utils';
import {ToolbarModule} from "primeng/toolbar";
import {TooltipModule} from "primeng/tooltip";
import { StatutRapprochement } from '../../enums/StatutRapprochement';

@Component({
  selector: 'app-details-rapprochement',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ToastModule,
    ConfirmDialogModule,
    CardModule,
    ButtonModule,
    TagModule,
    BreadcrumbModule,
    InputTextModule,
    CalendarModule,
    MultiSelectModule,
    DialogModule,
    Ripple,
    ToolbarModule,
    TooltipModule,
    SourceBadgeComponent,
    SelectButtonModule,
  ],
  templateUrl: './details-rapprochement.component.html',
  styleUrls: ['./details-rapprochement.component.scss'],
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class DetailsRapprochementComponent implements OnInit, OnDestroy {

  // Exposé au template pour éviter les comparaisons sur strings brutes
  readonly StatutRapprochement = StatutRapprochement;

  @ViewChild('dt') dt: Table | undefined;

  rapprochementId!: number;
  rapprochementDetails: RapprochementResponse | null = null;
  items: MenuItem[] | undefined;

  // Pagination et chargement des transactions
  transactions: TransactionResponseDto[] = [];
  totalTransactionsRecords: number = 0;
  loadingTransactions: boolean = false;
  rows: number = 10;

  // Formulaire de recherche
  searchForm!: FormGroup;
  transactionSourceOptions: any[] = [];

  displayEditDialog: boolean = false;
  editDateForm!: FormGroup;

  // Export PDF filtré (mêmes filtres que le tableau des transactions ci-dessous)
  exportingReport: boolean = false;
  showReportTypeModal: boolean = false;
  reportTypeControl = new FormControl<'FINANCE' | 'COMPTABILITE' | null>(null);
  reportTypeOptions = [
    { label: 'Finance', value: 'FINANCE', icon: 'pi pi-chart-bar', description: 'Inclut TVA et date d\'émission' },
    { label: 'Comptabilité', value: 'COMPTABILITE', icon: 'pi pi-book', description: 'Sans TVA ni date d\'émission' }
  ];

  private subscriptions: Subscription[] = [];

  private readonly route = inject(ActivatedRoute);
  private readonly rapprochementService = inject(RapprochementService);
  private readonly transactionService = inject(TransactionService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  ngOnInit(): void {
    this.populateTransactionSourceOptions();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.rapprochementId = +id;

      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'ID de rapprochement manquant.',
          life: 5000
        });
      }
    });
    this.initSearchForm();
    this.initEditDialogForm()
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initialise le formulaire de recherche.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: [''],
      observation: [''],
      montant: [null],
      transactionSources: [[]],
      dateOperationDebut: [null],
      dateOperationFin: [null]
    });

    // Écouter les changements du formulaire avec debounce
    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => this.onSearch());

    this.subscriptions.push(formValueChangesSub);

    // Charger les données initiales
    this.loadRapprochementDetails();
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

  /**
   * Remplit les options pour le multi-select des sources de transaction.
   */
  populateTransactionSourceOptions(): void {
    this.transactionSourceOptions = Object.entries(TransactionSource).map(([key, value]) => ({
      label: value,
      value: key
    }));
  }

  /**
   * Charge les détails du rapprochement avec les transactions filtrées et paginées.
   */
  loadRapprochementDetails(page: number = 0, size: number = this.rows, sort: string = 'dateEmission,desc'): void {
    if (!this.rapprochementId) return;

    this.loadingTransactions = true;

    const searchDto: TransactionSearchDto = this.searchForm ? this.searchForm.value : {};

    this.rapprochementService.getRapprochementById(this.rapprochementId, searchDto, page, size, sort)
      .subscribe({
        next: (data: RapprochementResponse) => {
          this.rapprochementDetails = data;
          this.transactions = data.transactions.content;
          this.totalTransactionsRecords = data.transactions.page.totalElements;
          this.loadingTransactions = false;
          this.updateBreadcrumb();
        },
        error: () => {
          this.loadingTransactions = false;
        }
      });
  }

  /**
   * Gère l'événement de chargement paresseux de la table de transactions.
   */
  loadTransactionsLazy(event: TableLazyLoadEvent): void {
    if (!this.rapprochementId) return;

    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = event.sortField ? `${event.sortField},${sortOrder}` : 'dateEmission,desc';

    this.loadRapprochementDetails(page, size, sort);
  }

  /**
   * Déclenche une nouvelle recherche en réinitialisant la pagination.
   */
  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadTransactionsLazy(this.dt.createLazyLoadMetadata());
    }
  }



  /**
   * Réinitialise le formulaire de recherche.
   */
  clearSearch(): void {
    this.searchForm.reset({
      libelle: '',
      observation: '',
      montant: null,
      transactionSources: [],
      dateOperationDebut: null,
      dateOperationFin: null
    });

    if (this.dt) {
      this.dt.first = 0;
      this.loadTransactionsLazy(this.dt.createLazyLoadMetadata());
    }
  }

  /**
   * Ouvre la modal de choix du type de rapport (Finance/Comptabilité) pour exporter en PDF les
   * transactions correspondant aux filtres actuellement appliqués sur cet écran.
   */
  exportReport(): void {
    if (!this.rapprochementId) {
      return;
    }
    this.reportTypeControl.setValue('FINANCE');
    this.showReportTypeModal = true;
  }

  confirmExportReport(): void {
    if (!this.rapprochementId || !this.reportTypeControl.value) return;

    const reportType = this.reportTypeControl.value;
    const searchDto: TransactionSearchDto = this.searchForm ? this.searchForm.value : {};

    this.showReportTypeModal = false;
    this.exportingReport = true;

    this.rapprochementService.downloadReport(this.rapprochementId, reportType, searchDto)
      .pipe(
        catchError(() => {
          return throwError(() => new Error('Download failed'));
        }),
        finalize(() => {
          this.exportingReport = false;
          this.reportTypeControl.setValue(null);
        })
      )
      .subscribe((response: Blob) => {
        const fileURL = URL.createObjectURL(response);
        const link = document.createElement('a');
        link.href = fileURL;
        const banqueNom = this.rapprochementDetails?.banqueNom ?? this.rapprochementId;
        const date = this.rapprochementDetails?.dateRapprochement ?? '';
        link.download = `Rapport_Rapprochement_${banqueNom}_${date}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(fileURL);

        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Rapport ${reportType} téléchargé avec succès.`,
          life: 3000
        });
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

  closeEditDialog(): void {
    this.displayEditDialog = false;
    this.editDateForm.reset()
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
        }
      });
  }



  /**
   * Met à jour le fil d'Ariane avec les informations spécifiques du rapprochement.
   */
  updateBreadcrumb(): void {
    if (this.rapprochementDetails) {
      this.items = [
        { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
        { icon: 'pi pi-history', route: '/portail/rapprochement/historique', label: 'Historique des rapprochements' },

      ];
    }
  }


}
