import { Component, inject, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { SourceBadgeComponent } from '../../../../global/shared-components/source-badge/source-badge.component';
import { TransactionResponseDto } from '../../../../transactions/dtos/TransactionResponseDto';
import { CommonModule, DatePipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import {ReactiveFormsModule, FormBuilder, FormGroup, Validators} from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {ConfirmationService, MessageService} from 'primeng/api';
import { ActivatedRoute } from '@angular/router';
import { TransactionSearchDto } from '../../../../transactions/dtos/TransactionSearchDto';
import { TransactionService } from "../../../../transactions/service/TransactionService";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {MultiSelectModule} from "primeng/multiselect";
import {TransactionSource, TransactionSourceLabels} from "../../../../transactions/dtos/TransactionSource";
import {AgreratedTransactionResponseDto} from "../../../../transactions/dtos/AgreratedTransactionResponseDto";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {ValidationService} from "../../../services/validation/validation.service";
import {BankStateService} from "../../../../comptes-bancaire/services/bank-state.service";
import {parseLocalDate, toLocalDateString} from "../../../../global/types/date.utils";
import {TransactionUpdateRequestDto} from "../../../../transactions/dtos/TransactionUpdateRequestDto";
import { SkeletonModule } from 'primeng/skeleton';
import { RippleModule } from 'primeng/ripple';
import {RapprochementService} from "../../../../rapprochement/service/rapprochement-service.service";
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DividerModule } from 'primeng/divider';
import { TvaInputComponent } from '../shared/tva-input/tva-input.component';
import { SaisieTva, saisieTvaParDefaut, messageErreurTva } from '../shared/tva-input/saisie-tva.model';


@Component({
  selector: 'app-rapprochemnt-encours',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    ToastModule,
    InputTextModule,
    ReactiveFormsModule,
    DatePipe,
    ConfirmDialogModule,
    MultiSelectModule,
    DialogModule,
    CalendarModule,
    SourceBadgeComponent,
    SkeletonModule,
    RippleModule,
    InputTextareaModule,
    DividerModule,
    TvaInputComponent,
  ],
  templateUrl: './rapprochemnt-encours.component.html',
  styleUrls: ['./rapprochemnt-encours.component.scss'],
  providers: [MessageService,ConfirmationService]
})
export class RapprochemntEncoursComponent implements OnInit, OnDestroy {
  @ViewChild('dt') dt!: Table;

  agregatedData: AgreratedTransactionResponseDto | null = null;

  /** Barre de filtres repliée par défaut — même comportement que l'écran Récapitulatifs. */
  showFilters = false;
  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  banqueIdFromRoute!: number;
  currentMonthYear: string | null = null;

  selectedRapprochementId: number | null = null;
  rapprochementStatut: 'EN_COURS' | 'CLOTURE_PROVISOIRE' | null = null;


  searchForm!: FormGroup;

  transactionSourceOptions: any[] = [];

  displayEditDialog: boolean = false;
  editDateForm!: FormGroup;
  /** Libellé du champ TVA — « TVA Déductible » en décaissement, « TVA Collectée » en encaissement. */
  editTvaLabel = 'TVA Déductible';
  /** Montant TTC de l'opération éditée (debit ou credit) — borne haute du champ TVA. */
  editMontantMaxTva: number | null = null;
  readonly messageErreurTva = messageErreurTva;

  private subscriptions: Subscription[] = [];

  private readonly transactionService = inject(TransactionService);
  private readonly rapprochementService = inject(RapprochementService);
  private readonly fb = inject(FormBuilder);
  private readonly validationService = inject(ValidationService);
  private readonly messageService = inject(MessageService);
  private readonly route = inject(ActivatedRoute);
  private readonly datePipe = inject(DatePipe);
  private readonly bankStateService = inject(BankStateService);

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
    this.transactionSourceOptions = Object.values(TransactionSource)
      .map(source => ({
        label: TransactionSourceLabels[source] ?? source,
        value: source
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'fr'));
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
        }
      });
  }

  /** Ouvre / referme la barre de filtres. */
  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  /** Résumé des critères actifs, affiché en pastilles quand la barre est repliée. */
  get activeFilterChips(): string[] {
    if (!this.searchForm) {
      return [];
    }
    const value = this.searchForm.value;
    const chips: string[] = [];

    if (value.transactionSources && value.transactionSources.length > 0) {
      chips.push(value.transactionSources.length === 1
        ? 'Source : ' + (TransactionSourceLabels[value.transactionSources[0] as TransactionSource]
                         ?? value.transactionSources[0])
        : 'Sources : ' + value.transactionSources.length);
    }
    if (value.libelle) {
      chips.push('Libellé : ' + value.libelle);
    }
    if (value.observation) {
      chips.push('Observation : ' + value.observation);
    }
    if (value.montant !== null && value.montant !== undefined && value.montant !== '') {
      chips.push('Montant : ' + value.montant);
    }
    const bornes = (value.dateOperationDebut ? 1 : 0) + (value.dateOperationFin ? 1 : 0);
    if (bornes > 0) {
      chips.push('Date d\'opération : ' + bornes + (bornes > 1 ? ' bornes' : ' borne'));
    }
    return chips;
  }

  /** Nombre de critères actifs — badge du bouton « Filtres ». */
  get activeFilterCount(): number {
    return this.activeFilterChips.length;
  }

  /** Remet tous les critères à leur valeur neutre ; le rechargement suit via valueChanges. */
  clearSearch(): void {
    this.searchForm.reset({
      libelle: '',
      transactionSources: [],
      observation: '',
      montant: null,
      dateOperationDebut: null,
      dateOperationFin: null
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
        this.bankStateService.notifyValidation();
      }
    });
  }

  openEditDialog(transaction: TransactionResponseDto): void {
    const estDecaissement = Number(transaction.debit) > 0;
    this.editTvaLabel = estDecaissement ? 'TVA Déductible' : 'TVA Collectée';
    this.editMontantMaxTva = estDecaissement ? Number(transaction.debit) : Number(transaction.credit);

    // La TVA stockée n'est qu'un montant déjà résolu (pas le taux d'origine) : préremplie comme
    // 0 % si nulle/nulle, sinon comme une saisie libre de ce montant — la seule représentation
    // fidèle de l'état actuel sans inventer un taux qui n'a pas été conservé.
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
  closeEditDialog(): void {
    this.displayEditDialog = false;
    this.editDateForm.reset()
  }

  exportPdf(): void {
    if (!this.banqueIdFromRoute || !this.selectedRapprochementId) {
      return;
    }
    this.exporting = true;
    const searchDto: TransactionSearchDto = {
      ...(this.searchForm.value as TransactionSearchDto),
      rapprochementId: this.selectedRapprochementId
    };

    this.transactionService.exportBankTransactionsPdf(this.banqueIdFromRoute, searchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rapprochement_transactions_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

}
