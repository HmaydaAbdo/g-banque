import {Component, inject, OnInit, ViewChild} from '@angular/core';
import {MiseADispositionResponseDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MiseADispositionSearchDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionSearchDto";
import {MiseADispositionService} from "../../../../../Mise-à-disposition/service/MiseADispositionService";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {ConfirmationService, MessageService, PrimeTemplate} from "primeng/api";
import {DatePipe, DecimalPipe} from "@angular/common";
import {ValidationService} from "../../../../services/validation/validation.service";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {
  DonneesComplementairesRapprochementDTO
} from "../../../../dtos/DonneesComplementairesRapprochementDTO";
import {
  MiseAdispositionTransactionRequestDto
} from "../../../../dtos/mise_a_disposition/MiseAdispositionTransactionRequestDto";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {EncoursMiseAdisposition} from "../../../../../Mise-à-disposition/dtos/EncoursMiseAdisposition";
import {Button} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DialogModule} from "primeng/dialog";
import {DividerModule} from "primeng/divider";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {InputTextareaModule} from "primeng/inputtextarea";
import {Ripple} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";
import {BatchValidationResponseDto} from "../../../../dtos/lot/BatchValidationResponseDto";
import {ValidationLotModalComponent} from "../../shared/validation-lot-modal/validation-lot-modal.component";
import {TvaInputComponent} from "../../shared/tva-input/tva-input.component";
import {
  decrireSaisieTva,
  messageErreurTva,
  SaisieTva,
  saisieTvaParDefaut
} from "../../shared/tva-input/saisie-tva.model";
import {TableSelectionState} from "../../shared/table-selection.state";

@Component({
  selector: 'app-mise-a-dispo-encaissement-encours',
  standalone: true,
  imports: [
    Button,
    CalendarModule,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DialogModule,
    DividerModule,
    DropdownModule,
    FormsModule,
    InputTextModule,
    InputTextareaModule,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    TableModule,
    ToastModule,
    TooltipModule,
    RouterLink,
    ValidationLotModalComponent,
    TvaInputComponent,
  ],
  templateUrl: './mise-a-dispo-encaissement-encours.component.html',
  styleUrl: './mise-a-dispo-encaissement-encours.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class MiseADispoEncaissementEncoursComponent implements OnInit {
  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Properties for the reconciliation modal
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedMiseADisposition: MiseADispositionResponseDto | null = null;

  // New properties for search and TVA
  searchForm!: FormGroup;
  currentSearchDto: MiseADispositionSearchDto = {};

  isSubmitting: boolean = false;

  /** Exposé au template pour le message d'erreur du champ TVA. */
  readonly messageErreurTva = messageErreurTva;

  /** Borne haute du montant de TVA saisissable, exposée au template. */
  get montantMaxTva(): number | null {
    return this.selectedMiseADisposition?.montant ?? null;
  }

  // Validation en lot
  selectionState = new TableSelectionState<MiseADispositionResponseDto>(m => m.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;
  readonly SensOperation = SensOperation;

  get pageSelection(): MiseADispositionResponseDto[] {
    return this.selectionState.getPageSelection(this.misesADisposition);
  }

  onSelectionChange(selection: MiseADispositionResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.misesADisposition, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(m => m.montant);
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  onLotValidated(response: BatchValidationResponseDto): void {
    this.selectionState.clear();
    this.loadOutstandingMisesADisposition();
    this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
    this.bankStateService.notifyValidation();
  }

  private readonly miseADispositionService = inject(MiseADispositionService);
  private readonly route = inject(ActivatedRoute);
  private readonly messageService = inject(MessageService);
  private readonly datePipe = inject(DatePipe);
  private readonly rapprochementService = inject(ValidationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingMisesADisposition();
      }
    });
    this.initSearchForm();
    this.initRapprochementForm();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      dateEmission: [null],
      emetteur: [''],
      beneficiaire: [''],
      montant:[null]
    });

    // Subscribe to form value changes with debounce
    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: MiseADispositionSearchDto) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingMisesADisposition();
      });
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tva: [saisieTvaParDefaut()],
      libelle: ['', Validators.required],
    });
  }

  openRapprochementModal(miseADisposition: MiseADispositionResponseDto): void {
    this.selectedMiseADisposition = miseADisposition;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(miseADisposition.dateEmission)!,
      libelle:miseADisposition.libelle,
      observation:miseADisposition.observation,
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

    if (this.rapprochementForm.valid && this.selectedMiseADisposition) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedMiseADisposition = this.selectedMiseADisposition;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher cette Mise à Disposition ?
            <br><br>
            <strong>Emetteur :</strong> ${selectedMiseADisposition.emetteur}
            <br>
            <strong>Bénéficiaire:</strong> ${selectedMiseADisposition.beneficiaire}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedMiseADisposition.montant,'1.2-2','fr-FR')} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Collectée:</strong> ${decrireSaisieTva(formValue.tva)}</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Mise à Disposition',
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

    if (this.rapprochementForm.valid && this.selectedMiseADisposition) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;
      const saisieTva: SaisieTva = this.rapprochementForm.value.tva;
      const rapprochementId = this.bankStateService.getRapprochementId();

      const miseADispositionTransactionRequest: MiseAdispositionTransactionRequestDto = {
        id: this.selectedMiseADisposition.id,
        dateEmission:this.datePipe.transform(donneesComplementaires.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(donneesComplementaires.dateValeur,'yyyy-MM-dd') as string,
        dateOperation:this.datePipe.transform(donneesComplementaires.dateOperation,'yyyy-MM-dd') as string,
        observation: donneesComplementaires.observation,
        tauxTva: saisieTva.tauxTva,
        montantTva: saisieTva.montantTva,
        transactionSource: TransactionSource.MiseADisposition,
        libelle: donneesComplementaires.libelle,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;
      this.rapprochementService.miseAdispoEncaissementToRapprochement(miseADispositionTransactionRequest)
        .subscribe({
          next: (response) => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Mise à disposition rapprochée avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingMisesADisposition();
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

  /**
   * Loads the outstanding mise à dispositions for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  clearSearch(): void {
    this.searchForm.reset({ dateEmission: null, emetteur: '', beneficiaire: '', montant: null });
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadOutstandingMisesADisposition(this.dt.createLazyLoadMetadata());
    }
  }

  loadOutstandingMisesADisposition(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.miseADispositionService
      .getEncoursEncaissementsForMiseADispositionByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursMiseAdisposition) => {
          this.misesADisposition = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
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
    this.miseADispositionService.exportEncoursEncaissementPdf(this.banqueIdFromRoute, this.currentSearchDto).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mises_a_disposition_encours_encaissement_${new Date().toISOString().slice(0, 10)}.pdf`;
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

