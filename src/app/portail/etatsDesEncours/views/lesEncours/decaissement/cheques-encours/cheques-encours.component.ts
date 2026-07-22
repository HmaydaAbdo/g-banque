import {Component, inject, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {ConfirmationService, MessageService} from 'primeng/api';
import {ChequeResponseDto} from "../../../../../cheques/dtos/ChequeResponseDto";
import {ChequeService} from "../../../../../cheques/services/ChequeService";
import {Ripple} from "primeng/ripple";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ValidationService} from "../../../../services/validation/validation.service";
import {DonneesComplementairesRapprochementDTO} from "../../../../dtos/DonneesComplementairesRapprochementDTO";
import {ChequeTransactionRequest} from "../../../../dtos/cheques/ChequeTransactionRequest";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {DividerModule} from "primeng/divider";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {EncoursChequeResponseDto} from "../../../../../cheques/dtos/EncoursChequeResponseDto";
import {EncoursChequeSearchDto} from "../../../../../cheques/dtos/EncoursChequeSearchDto";
import {InputTextModule} from "primeng/inputtext";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";
import {BatchValidationResponseDto} from "../../../../dtos/lot/BatchValidationResponseDto";
import {ValidationLotModalComponent} from "../../shared/validation-lot-modal/validation-lot-modal.component";
import {TableSelectionState} from "../../shared/table-selection.state";

@Component({
  selector: 'app-cheques-encours',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    CardModule,
    ButtonModule,
    ToastModule,
    TooltipModule,
    PaginatorModule,
    DatePipe,
    Ripple,
    DividerModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ConfirmDialogModule,
    ValidationLotModalComponent,
  ],
  templateUrl: './cheques-encours.component.html',
  styleUrl: './cheques-encours.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class ChequesEncoursComponent implements OnInit {
  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // New properties for search
  searchForm!: FormGroup;
  currentSearchDto: EncoursChequeSearchDto = {};

  // Properties for the validation modal
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedCheque: ChequeResponseDto | null = null;

  // Updated property for TVA options to use label-value pairs
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  // Validation en lot
  selectionState = new TableSelectionState<ChequeResponseDto>(c => c.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;
  readonly SensOperation = SensOperation;

  get pageSelection(): ChequeResponseDto[] {
    return this.selectionState.getPageSelection(this.cheques);
  }

  onSelectionChange(selection: ChequeResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.cheques, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(c => c.montant);
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  onLotValidated(response: BatchValidationResponseDto): void {
    this.selectionState.clear();
    this.loadOutstandingCheques();
    this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
    this.bankStateService.notifyValidation();
  }

  private readonly chequeService = inject(ChequeService);
  private readonly rapprochementService = inject(ValidationService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly messageService = inject(MessageService);
  private readonly ConfirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingCheques();
      }
    });
    this.initSearchForm();
    this.initRapprochementForm();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numero: [''],
      beneficiaire: [''],
      montant:[null]
    });

    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: EncoursChequeSearchDto) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingCheques();
      });
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tvaDeductible: [0, Validators.required],
      libelle: ['', Validators.required]
    });
  }

  openRapprochementModal(cheque: ChequeResponseDto): void {
    this.selectedCheque = cheque;
    this.initRapprochementForm();

    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(cheque.dateEmission)! ,
      observation: cheque.observation,
      libelle: cheque.libelle
    });
    this.displayModal = true;
  }


  confirmRapprochement(): void {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.'
      });
      return;
    }

    if (this.rapprochementForm.valid && this.selectedCheque) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés
      const formValue = this.rapprochementForm.getRawValue();
      const selectedCheque = this.selectedCheque;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce chèque ?
            <br><br>
            <strong>N° Chèque:</strong> ${selectedCheque.numero}
            <br>
            <strong>Bénéficiaire:</strong> ${selectedCheque.beneficiaire}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedCheque.montant,"1.2-2","fr-FR")} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Déductible:</strong> ${formValue.tvaDeductible}%</li>
            </ul>
      `;

      this.ConfirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Chèque',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {
          // L'utilisateur a annulé, aucune action
        }
      });
    }
  }



  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedCheque) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;
      const rapprochementId = this.bankStateService.getRapprochementId();

      const chequeTransactionRequest: ChequeTransactionRequest = {
        idCheque: this.selectedCheque.id,
        dateEmission: this.datePipe.transform(donneesComplementaires.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(donneesComplementaires.dateValeur,'yyyy-MM-dd') as string,
        dateOperation: this.datePipe.transform(donneesComplementaires.dateOperation,'yyyy-MM-dd') as string,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.CHEQUE,
        libelle:donneesComplementaires.libelle,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;

      this.rapprochementService.chequeDecaissementToRapprochement(chequeTransactionRequest)
        .subscribe({
          next: (response) => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Chèque rapproché avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingCheques();
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
            this.bankStateService.notifyValidation();

          },
          error: (error) => {
            this.isSubmitting = false;
          }
        });
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  loadOutstandingCheques(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;

    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.chequeService
      .getEncoursDecaissementChequesByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursChequeResponseDto) => {
          this.cheques = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error: Error) => {
          this.loading = false;
        },
      });
  }

  exportPdf(): void {
    this.exporting = true;
    this.chequeService.exportEncoursDecaissementPdf(this.banqueIdFromRoute, this.currentSearchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `cheques_encours_decaissement_${new Date().toISOString().slice(0, 10)}.pdf`;
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
