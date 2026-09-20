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
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DividerModule } from 'primeng/divider';
import { TvaInputComponent } from '../../../etatsDesEncours/views/lesEncours/shared/tva-input/tva-input.component';
import { SaisieTva, saisieTvaParDefaut, messageErreurTva } from '../../../etatsDesEncours/views/lesEncours/shared/tva-input/saisie-tva.model';

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
    InputTextareaModule,
    DividerModule,
    TvaInputComponent,
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

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  displayEditDialog: boolean = false;
  editDateForm!: FormGroup;
  /** Libellé du champ TVA — « TVA Déductible » en décaissement, « TVA Collectée » en encaissement. */
  editTvaLabel = 'TVA Déductible';
  /** Montant TTC de l'opération éditée (debit ou credit) — borne haute du champ TVA. */
  editMontantMaxTva: number | null = null;
  readonly messageErreurTva = messageErreurTva;

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
      dateEmissionDebut: [null],
      dateEmissionFin: [null],
      dateOperationDebut: [null],
      dateOperationFin: [null],
      dateValeurDebut: [null],
      dateValeurFin: [null]
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

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEmissionDebut || v.dateEmissionFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
  }
  /**
   * Mêmes champs, et la même politique TVA, que le dialogue de validation générique du récap
   * (architecture.md, Règle 2) : dateEmission verrouillée (informative), pas de banque.
   */
  initEditDialogForm(): void {
    this.editDateForm = this.fb.group({
      id: [null],
      dateEmission: [{ value: null, disabled: true }],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      libelle: ['', Validators.required],
      observation: ['', Validators.required],
      tva: [saisieTvaParDefaut()],
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
      dateEmissionDebut: null,
      dateEmissionFin: null,
      dateOperationDebut: null,
      dateOperationFin: null,
      dateValeurDebut: null,
      dateValeurFin: null
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
    const estDecaissement = Number(transaction.debit) > 0;
    this.editTvaLabel = estDecaissement ? 'TVA Déductible' : 'TVA Collectée';
    this.editMontantMaxTva = estDecaissement ? Number(transaction.debit) : Number(transaction.credit);

    // La TVA stockée n'est qu'un montant déjà résolu (pas le taux d'origine) : préremplie comme
    // 0 % si nulle, sinon comme une saisie libre de ce montant — la seule représentation fidèle
    // de l'état actuel sans inventer un taux qui n'a pas été conservé.
    const tvaActuelle = estDecaissement ? Number(transaction.tvaDeductible) : Number(transaction.tvaCollectee);
    const tvaInitiale: SaisieTva = tvaActuelle > 0
      ? { tauxTva: null, montantTva: tvaActuelle }
      : { tauxTva: 0, montantTva: null };

    this.editDateForm.reset();
    this.editDateForm.patchValue({
      id: transaction.id,
      dateEmission: parseLocalDate(transaction.dateEmission),
      dateValeur: parseLocalDate(transaction.dateValeur),
      dateOperation: parseLocalDate(transaction.dateOperations),
      libelle: transaction.libelle,
      observation: transaction.observation,
      tva: tvaInitiale,
    });
    this.displayEditDialog = true;
  }

  isEditInvalid(controlName: string): boolean {
    const control = this.editDateForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  closeEditDialog(): void {
    this.displayEditDialog = false;
    this.editDateForm.reset()
  }

  updateTransaction(): void {
    if (!this.editDateForm.valid) return;

    const formValue = this.editDateForm.getRawValue();
    const saisieTva: SaisieTva = formValue.tva;
    const updateDto: TransactionUpdateRequestDto = {
      dateValeur: toLocalDateString(formValue.dateValeur)!,
      dateOperation: toLocalDateString(formValue.dateOperation)!,
      libelle: formValue.libelle,
      observation: formValue.observation,
      tauxTva: saisieTva.tauxTva,
      montantTva: saisieTva.montantTva,
    };

    const transactionId = formValue.id;
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
