import {Component, OnInit, ViewChild} from '@angular/core';
import {MiseADispositionResponseDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MiseADispositionSearchDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionSearchDto";
import {MiseADispositionService} from "../../../../../Mise-à-disposition/service/MiseADispositionService";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {ConfirmationService, MessageService, PrimeTemplate} from "primeng/api";
import {DatePipe, DecimalPipe, NgIf} from "@angular/common";
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
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    TableModule,
    ToastModule,
    TooltipModule,
    RouterLink
  ],
  templateUrl: './mise-a-dispo-encaissement-encours.component.html',
  styleUrl: './mise-a-dispo-encaissement-encours.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class MiseADispoEncaissementEncoursComponent implements OnInit {
  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
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
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;


  constructor(
    private miseADispositionService: MiseADispositionService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
    private rapprochementService: ValidationService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private decimalPipe: DecimalPipe,
    private bankStateService: BankStateService
  ) {
  }

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
      tvaDeductible: [0, Validators.required],
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
                <li><strong>TVA Collectée:</strong> ${formValue.tvaDeductible}%</li>
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
      const rapprochementId = this.route.snapshot.queryParamMap.get('rapprochementId');

      const miseADispositionTransactionRequest: MiseAdispositionTransactionRequestDto = {
        id: this.selectedMiseADisposition.id,
        dateEmission:this.datePipe.transform(donneesComplementaires.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(donneesComplementaires.dateValeur,'yyyy-MM-dd') as string,
        dateOperation:this.datePipe.transform(donneesComplementaires.dateOperation,'yyyy-MM-dd') as string,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.MiseADisposition,
        libelle: donneesComplementaires.libelle,
        rapprochementId: +rapprochementId!
      };

      this.isSubmitting = true;
      this.rapprochementService.miseAdispoEncaissementToRapprochement(miseADispositionTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Mise à disposition rapprochée avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingMisesADisposition();
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
          },
          error: (error) => {
            console.error('Erreur lors du validation de la mise à disposition:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message || 'Une erreur est survenue lors du validation.'
            });
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
          console.error(
            'Erreur lors du chargement des mises à disposition en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des mises à disposition en cours.',
          });
        },
      });
  }
}

