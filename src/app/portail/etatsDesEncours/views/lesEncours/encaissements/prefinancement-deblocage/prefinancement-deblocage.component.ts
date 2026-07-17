import { Component, inject, OnInit, ViewChild, OnDestroy } from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';
import { RippleModule } from 'primeng/ripple';
import {ConfirmationService, MessageService} from 'primeng/api';
import { PrefinancementResponseDTO } from "../../../../../prefinancements/dtos/PrefinancementResponseDTO";
import { PrefinancementService } from "../../../../../prefinancements/service/PrefinancementService";
import { EncoursPrefinancementDto } from "../../../../../prefinancements/dtos/EncoursPrefinancementDto";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { ValidationService } from "../../../../services/validation/validation.service";
import { PrefinancementTransactionRequest } from "../../../../dtos/prefinancement/PrefinancementTransactionRequest";
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";
import { InputTextModule } from 'primeng/inputtext';
import { EncoursPrefinancementSearchDto } from "../../../../../prefinancements/dtos/EncoursPrefinancementSearchDto";
import { DropdownModule } from 'primeng/dropdown';
import {ActivatedRoute, RouterLink} from '@angular/router';

import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {InputTextareaModule} from "primeng/inputtextarea";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";

@Component({
  selector: 'app-prefinancement-deblocage',
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
    RippleModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextModule,
    DropdownModule,
    InputTextareaModule,
    ConfirmDialogModule,
    RouterLink,
    ValidationLotModalComponent,
  ],
  templateUrl: './prefinancement-deblocage.component.html',
  styleUrl: './prefinancement-deblocage.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class PrefinancementDeblocageComponent implements OnInit, OnDestroy {
  prefinancements: PrefinancementResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueId!: number;
  totalEncours!: number;
  @ViewChild('dt') dt: Table | undefined;

  // Search form properties
  searchForm!: FormGroup;
  private searchSubscription!: Subscription;

  // properties for the dialog and form
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedPrefinancement: PrefinancementResponseDTO | null = null;

  // TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  isSubmitting: boolean = false;

  // Validation en lot
  selectionState = new TableSelectionState<PrefinancementResponseDTO>(p => p.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): PrefinancementResponseDTO[] {
    return this.selectionState.getPageSelection(this.prefinancements);
  }

  onSelectionChange(selection: PrefinancementResponseDTO[]): void {
    this.selectionState.onPageSelectionChange(this.prefinancements, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(p => p.montant);
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  onLotValidated(response: BatchValidationResponseDto): void {
    this.selectionState.clear();
    this.loadEncoursDeblocages();
    this.bankStateService.refreshBankInfo(this.banqueId);
    this.bankStateService.notifyValidation();
  }

  private readonly prefinancementService = inject(PrefinancementService);
  private readonly messageService = inject(MessageService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly rapprochementService = inject(ValidationService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Aucun ID de banque trouvé dans l\'URL.'
        });
      }
    });
    this.initSearchForm();
    this.initRapprochementForm();
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }

  /**
   * Initializes the reactive form for the search criteria.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      marcheNumero: [''],
      marcheName: [''],
      montant: [null],
    });



    this.searchSubscription = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        if (this.dt) {
          this.dt.first = 0;
          this.loadEncoursDeblocages();
        }
      });
  }

  /**
   * Initializes the reactive form for the validation data.
   */
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tvaCollecte: [null, Validators.required],
      observation:['',Validators.required],
      libelle:['',Validators.required],
    });
  }

  /**
   * Opens the dialog to validate a prefinancement.
   * @param prefinancement The prefinancement record to validate.
   */
  openRapprochementModal(prefinancement: PrefinancementResponseDTO): void {
    this.selectedPrefinancement = prefinancement;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission:parseLocalDate(prefinancement.dateEmission)!,
      observation:prefinancement.observation,
      libelle:prefinancement.libelle
    })
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

    if (this.rapprochementForm.valid && this.selectedPrefinancement) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedPrefinancement = this.selectedPrefinancement;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');



      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce Déblocage de Préfinancement ?
            <br><br>
            <strong>N° de Marché:</strong> ${selectedPrefinancement.marcheNumero}
            <br>
            <strong>Montant Débloqué:</strong> ${this.decimalPipe.transform(selectedPrefinancement.montant,'1.2-2','fr-FR')} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Collectée:</strong> ${formValue.tvaCollecte}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Déblocage Préfinancement',
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

  /**
   * Handles the form submission to send the validation request.
   */
  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedPrefinancement) {


    const rapprochementId = this.bankStateService.getRapprochementId();
    const prefinancementTransactionRequest: PrefinancementTransactionRequest = {
      prefinancementId: this.selectedPrefinancement.id,
      banqueId: this.selectedPrefinancement.banqueId,
      dateEmission:this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
      dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
      dateValeur: this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
      tva: this.rapprochementForm.value.tvaCollecte,
      libelle:this.rapprochementForm.value.libelle,
      observation:this.rapprochementForm.value.observation,
      rapprochementId: rapprochementId!
    };

    this.isSubmitting = true;

    this.rapprochementService.prefinancementToRapprochement(prefinancementTransactionRequest)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Préfinancement validé et rapproché avec succès.',
          });
          this.isSubmitting = false;
          this.loadEncoursDeblocages();
          this.displayModal = false;
          this.bankStateService.refreshBankInfo(this.banqueId);
          this.bankStateService.notifyValidation();

        },
        error: (error) => {
          this.isSubmitting = false;

        },
      });
  }}



  /**
   * Loads outstanding pre-financing disbursement records.
   */
  loadEncoursDeblocages(event?: TableLazyLoadEvent): void {
    this.loading = true;

    if (!this.banqueId) {
      this.loading = false;
      return;
    }

    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursPrefinancementSearchDto = this.searchForm.value;

    this.prefinancementService
      .getEncoursDeblocage(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursPrefinancementDto) => {
          this.prefinancements = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  /**
   * Helper function to check if a form control is invalid.
   */
  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
