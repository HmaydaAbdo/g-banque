import {Component, EventEmitter, inject, Input, Output} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ConfirmationService, MessageService} from 'primeng/api';
import {DialogModule} from 'primeng/dialog';
import {CalendarModule} from 'primeng/calendar';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {DropdownModule} from 'primeng/dropdown';
import {DividerModule} from 'primeng/divider';
import {ButtonModule} from 'primeng/button';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {ToastModule} from 'primeng/toast';
import {TransactionSource} from '../../../../../transactions/dtos/TransactionSource';
import {SensOperation} from '../../../../../transactions/dtos/SensOperation';
import {BatchValidationRequestDto} from '../../../../dtos/lot/BatchValidationRequestDto';
import {BatchValidationResponseDto} from '../../../../dtos/lot/BatchValidationResponseDto';
import {ValidationService} from '../../../../services/validation/validation.service';
import {BankStateService} from '../../../../../comptes-bancaire/services/bank-state.service';

/**
 * Modale de validation (rapprochement) en lot, partagée par tous les écrans "Encours".
 * Applique les mêmes champs communs (TVA, date opération, date valeur) à toutes les opérations
 * sélectionnées, en un seul appel à POST /rapprochement/process/lot (voir BatchValidationService
 * côté backend pour le détail du dispatch et le comportement tout-ou-rien).
 *
 * Volontairement absent de ce formulaire : libellé, observation, date d'émission. Propres à
 * chaque opération, ils ne sont jamais saisis en lot — chaque opération conserve automatiquement
 * les siens côté backend (fallback sur l'entité source dans chaque mapper si le DTO unitaire ne
 * les porte pas). Seuls TVA, date opération et date valeur — qui proviennent du relevé bancaire
 * traité — sont légitimement communs à tout le lot.
 *
 * Le composant parent (chaque *-encours.component.ts) reste responsable de :
 * - la sélection multiple dans son p-table (dataKey + [(selection)]) ;
 * - fournir transactionSource (+ sens si le type distingue décaissement/encaissement, sauf LCN qui
 *   utilise directement LCN_DECAISSEMENT/LCN_ENCAISSEMENT comme transactionSource — voir
 *   BatchValidationRequestDto) et banqueId si le type l'exige (Decompte, DroitConstate, Factoring,
 *   Prefinancement) ;
 * - recharger sa liste et rafraîchir les infos bancaires après l'événement (validated).
 */
@Component({
  selector: 'app-validation-lot-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    CalendarModule,
    InputTextareaModule,
    DropdownModule,
    DividerModule,
    ButtonModule,
    ConfirmDialogModule,
    ToastModule,
  ],
  templateUrl: './validation-lot-modal.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe],
})
export class ValidationLotModalComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  @Input() transactionSource!: TransactionSource;
  @Input() sens?: SensOperation;
  @Input() banqueId?: number;
  @Input() selectedIds: number[] = [];
  @Input() montantTotal = 0;
  /** Utilisé dans les messages ("3 virements validés avec succès"). */
  @Input() libelleType = 'opérations';
  /**
   * Libellé du champ TVA, à faire correspondre à celui du modal unitaire du même écran
   * ("TVA Déductible" pour un décaissement, "TVA Collectée"/"TVA collectée" pour un
   * encaissement — voir le composant parent pour la valeur exacte déjà utilisée ailleurs
   * sur cet écran).
   */
  @Input() tvaLabel = 'TVA Déductible';

  @Output() validated = new EventEmitter<BatchValidationResponseDto>();

  rapprochementForm!: FormGroup;
  isSubmitting = false;
  tvaOptions: { label: string; value: number }[] = [
    {label: '0%', value: 0},
    {label: '10%', value: 10},
    {label: '14%', value: 14},
    {label: '20%', value: 20},
  ];

  private readonly fb = inject(FormBuilder);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly validationService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);

  constructor() {
    this.initForm();
  }

  private initForm(): void {
    this.rapprochementForm = this.fb.group({
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      tvaDeductible: [0, Validators.required],
    });
  }

  /** Réinitialise le formulaire — à appeler par le parent avant de passer visible à true. */
  reset(): void {
    this.initForm();
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  confirmValidation(): void {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.',
      });
      return;
    }

    const formValue = this.rapprochementForm.getRawValue();
    const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
    const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

    const recapMessage = `
          Êtes-vous sûr de vouloir valider ces ${this.selectedIds.length} ${this.libelleType} en une seule fois ?
          <br><br>
          <strong>Nombre d'opérations :</strong> ${this.selectedIds.length}
          <br>
          <strong>Montant total :</strong> ${this.decimalPipe.transform(this.montantTotal, '1.2-2', 'fr-FR')} MAD
          <br><br>
          <strong>Détails du rapprochement (appliqués à toutes les opérations sélectionnées) :</strong>
          <ul>
              <li><strong>Date de valeur :</strong> ${dateValeurFormatted || '-'}</li>
              <li><strong>Date d'opération :</strong> ${dateOperationFormatted || '-'}</li>
              <li><strong>${this.tvaLabel} :</strong> ${formValue.tvaDeductible}%</li>
          </ul>
          <br>
          Le libellé et l'observation d'origine de chaque opération sont conservés.
    `;

    this.confirmationService.confirm({
      message: recapMessage,
      header: 'Confirmation de validation en lot',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => this.submit(),
    });
  }

  private submit(): void {
    const rapprochementId = this.bankStateService.getRapprochementId();
    const formValue = this.rapprochementForm.getRawValue();

    const request: BatchValidationRequestDto = {
      transactionSource: this.transactionSource,
      sens: this.sens,
      banqueId: this.banqueId,
      ids: this.selectedIds,
      dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
      dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
      tvaDeductible: formValue.tvaDeductible,
      rapprochementId: rapprochementId!,
    };

    this.isSubmitting = true;
    this.validationService.validerEnLot(request).subscribe({
      next: (response) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `${response.nombreOperations} ${this.libelleType} validé(e)s avec succès.`,
        });
        this.isSubmitting = false;
        this.validated.emit(response);
        this.close();
      },
      error: (error) => {
        this.isSubmitting = false;
      },
    });
  }
}
