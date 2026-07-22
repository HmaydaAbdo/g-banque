import {Component, inject, OnInit, ViewChild, OnDestroy} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ConfirmationService, MessageService} from "primeng/api";
import {ValidationService} from "../../../../services/validation/validation.service";
import {DecompteResponseDto} from "../../../../../decomptes/dtos/DecompteResponseDto";
import {DecompteService} from "../../../../../decomptes/service/DecompteService";
import {DecompteTransactionRequest} from "../../../../dtos/decomptes/DecompteTransactionRequest";
import {EncoursDecompteDto} from "../../../../../decomptes/dtos/EncoursDecompteDto";
import {ButtonModule} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {DatePipe, CommonModule, DecimalPipe} from "@angular/common";
import {DialogModule} from "primeng/dialog";
import {RippleModule} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {InputTextModule} from "primeng/inputtext";
import {  Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { EncoursDecompteSearchDto } from "../../../../../decomptes/dtos/EncoursDecompteSearchDto";
import {DropdownModule} from "primeng/dropdown";
import {InputTextareaModule} from "primeng/inputtextarea";
import _default from "chart.js/dist/plugins/plugin.tooltip";
import numbers = _default.defaults.animations.numbers;
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";


@Component({
  selector: 'app-decomptes',
  standalone: true,
  imports: [
    ButtonModule,
    CalendarModule,
    DatePipe,
    DialogModule,
    ReactiveFormsModule,
    RippleModule,
    TableModule,
    ToastModule,
    TooltipModule,
    InputTextModule,
    DropdownModule,
    CommonModule,
    InputTextareaModule,
    ConfirmDialogModule,
    RouterLink,
    ValidationLotModalComponent,
  ],
  templateUrl: './decomptes.component.html',
  styleUrl: './decomptes.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe]
})
export class DecomptesComponent implements OnInit, OnDestroy {

  decomptes: DecompteResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueId!: number;
  totalEncours!: number;
  @ViewChild('dt') dt: Table | undefined;

  // New properties for search and TVA
  searchForm!: FormGroup;
  private searchSubscription!: Subscription;
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  //  properties for the dialog and form
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedDecompte: DecompteResponseDto | null = null;

  isSubmitting: boolean = false;

  /** Indicateur de chargement du bouton "Exporter PDF". */
  exporting = false;

  // Validation en lot
  selectionState = new TableSelectionState<DecompteResponseDto>(d => d.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): DecompteResponseDto[] {
    return this.selectionState.getPageSelection(this.decomptes);
  }

  onSelectionChange(selection: DecompteResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.decomptes, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(d => d.montant);
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  onLotValidated(response: BatchValidationResponseDto): void {
    this.selectionState.clear();
    this.loadEncoursDecomptes();
    this.bankStateService.refreshBankInfo(this.banqueId);
    this.bankStateService.notifyValidation();
  }

  private readonly decompteService = inject(DecompteService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly rapprochementService = inject(ValidationService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {
    // Subscribe to parent route parameters to get banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;
        this.initSearchForm();
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Aucun ID de banque trouvé dans l\'URL.'
        });
      }
    });

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
      montant: [null]
    });

    this.loadEncoursDecomptes();

    this.searchSubscription = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        if (this.dt) {
          this.dt.first = 0;
          this.loadEncoursDecomptes();
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
      tva: [null, Validators.required],
      libelle: ['', Validators.required],
      observation: ['', Validators.required],
    });
  }

  /**
   * Opens the dialog to validate a decompte.
   * @param decompte The decompte record to validate.
   */
  openRapprochementModal(decompte: DecompteResponseDto): void {
    this.selectedDecompte = decompte;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission:parseLocalDate(decompte.dateEmission)!,
      libelle:decompte.libelle,
      observation:decompte.observation
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

    if (this.rapprochementForm.valid && this.selectedDecompte) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedDecompte = this.selectedDecompte;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');



      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce Décompte ?
            <br><br>
            <strong>N° de Marché:</strong> ${selectedDecompte.marcheNumero}
            <br>
            <strong>Montant du Décompte:</strong> ${this.decimalPipe.transform(selectedDecompte.montant,'1.2-2','fr-FR')} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA :</strong> ${formValue.tva}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Décompte',
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

    if (this.rapprochementForm.valid && this.selectedDecompte) {


    const rapprochementId = this.bankStateService.getRapprochementId();
    // Construct the request DTO using the selected dc and form data
    const decompteTransactionRequest:DecompteTransactionRequest= {
      dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,  'yyyy-MM-dd') as string,
      dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,  'yyyy-MM-dd') as string,
      dateValeur: this.datePipe.transform(this.rapprochementForm.value.dateValeur,  'yyyy-MM-dd') as string,
      decompteId: this.selectedDecompte.id,
      banqueId: this.selectedDecompte.banqueEncaissementId,
      tva: this.rapprochementForm.value.tva,
      libelle:this.rapprochementForm.value.libelle,
      observation:this.rapprochementForm.value.observation,
      rapprochementId: rapprochementId!
    };

    this.isSubmitting = true;

    this.rapprochementService.decompteToRapprochement(decompteTransactionRequest)
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'décompte  validée et rapprochée avec succès.',
          });
          this.isSubmitting = false;
          this.loadEncoursDecomptes();
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
   * Loads outstanding dc disbursement records.
   * Handles pagination and sorting from the PrimeNG table.
   */
  loadEncoursDecomptes(event?: TableLazyLoadEvent): void {
    this.loading = true;

    // Guard clause: ensure banqueId is available before making the call
    if (!this.banqueId) {
      this.loading = false;
      return;
    }

    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursDecompteSearchDto = this.searchForm.value;

    this.decompteService
      .getEncoursDecomptes(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursDecompteDto) => {
          this.decomptes=response.page.content
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

  /**
   * Exporte en PDF l'intégralité de l'encours de décomptes correspondant aux filtres
   * actuellement appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    if (!this.banqueId) return;
    this.exporting = true;
    const searchCriteria: EncoursDecompteSearchDto = this.searchForm.value;
    this.decompteService.exportEncoursPdf(this.banqueId, searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `decomptes_encours_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      },
    });
  }

}
