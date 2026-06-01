import {Component, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {CommonModule, DatePipe, DecimalPipe} from "@angular/common";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {CardModule} from "primeng/card";
import {ButtonModule} from "primeng/button";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {PaginatorModule} from "primeng/paginator";
import {RippleModule} from "primeng/ripple";
import {DialogModule} from "primeng/dialog";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {CalendarModule} from "primeng/calendar";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {InputTextareaModule} from "primeng/inputtextarea";
import {ConfirmationService, MessageService} from "primeng/api";
import {Decimal} from "decimal.js";
import {Subscription} from "rxjs";
import {ValidationService} from "../../../../services/validation/validation.service";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {FactoringResponseDto} from "../../../../../factoring/dtos/FactoringResponseDto";
import {FactoringService} from "../../../../../factoring/service/FactoringService";
import {FactoringTransactionRequest} from "../../../../dtos/factoring/FactoringTransactionRequest";
import {EncoursFactoringSearchDto} from "../../../../../factoring/dtos/EncoursFactoringSearchDto";
import {EncoursFactoringDto} from "../../../../../factoring/dtos/EncoursFactoringDto";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-factoring-remboursements',
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
  ],
  templateUrl:'./factoring-remboursements.component.html',

  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class FactoringRemboursementsComponent implements OnInit, OnDestroy {
  factoringList: FactoringResponseDto[] = [];
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
  selectedFactoring: FactoringResponseDto | null = null;

  // New TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  isSubmitting: boolean = false;


  constructor(
    private factoringService: FactoringService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private rapprochementService: ValidationService,
    private fb: FormBuilder,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private route: ActivatedRoute ,
    private bankStateService: BankStateService

  ) {}

  ngOnInit(): void {
    // Subscribe to parent route parameters to get banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;
        // Call initSearchForm here to start the data loading process
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


  initSearchForm(): void {
    this.searchForm = this.fb.group({
      marcheNumero: [''],
      marcheName: [''],
      montant: [null],
    });

    // Initial data load triggered after banqueId is set
    this.loadEncoursRemboursements();

    // Setup valueChanges subscription after the form is initialized
    this.searchSubscription = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        if (this.dt) {
          this.dt.first = 0;
          this.loadEncoursRemboursements();
        }
      });
  }


  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tva: [null, Validators.required],
      observation:['',Validators.required],
      libelle:['',Validators.required],
    });
  }


  openRapprochementModal(factoring: FactoringResponseDto): void {
    this.selectedFactoring = factoring;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(factoring.dateEmission)!,
      libelle: factoring.libelle,
      observation : factoring.observation
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

    if (this.rapprochementForm.valid && this.selectedFactoring) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedFactoring = this.selectedFactoring;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce Factoring ?
            <br><br>
            <strong>N° de Marché:</strong> ${selectedFactoring.marcheNumero}
            <br>
            <strong>Montant du Factoring:</strong> ${this.decimalPipe.transform(selectedFactoring.montant,'1.2-2','fr-FR')} MAD
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
        header: 'Confirmation de Rapprochement Factoring',
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
    if (this.rapprochementForm.valid && this.selectedFactoring) {

      this.isSubmitting = true;
      const rapprochementId = this.bankStateService.getRapprochementId();
      const factoringTransactionRequest: FactoringTransactionRequest = {
        factoringId: this.selectedFactoring.id,
        banqueId: this.selectedFactoring.banqueId,
        dateEmission:this.datePipe.transform( this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string ,
        dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform( this.rapprochementForm.value.dateValeur,'yyyy-MM-dd' ) as string,
        tva: this.rapprochementForm.value.tva,
        libelle: this.rapprochementForm.value.libelle,
        observation: this.rapprochementForm.value.observation,
        rapprochementId: rapprochementId!
      };

      this.rapprochementService.factoringToRapprochement(factoringTransactionRequest)
        .subscribe({
          next: (response) => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Factoring validé et rapproché avec succès.',
            });
            this.dt?.reset();
            this.isSubmitting = false;
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueId);

          },
          error: (error) => {
            console.error('Erreur lors du validation du factoring:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error?.message || 'Une erreur est survenue lors du validation.',
            });
            this.isSubmitting = false;
          },
        });
    }
    }


  loadEncoursRemboursements(event?: TableLazyLoadEvent): void {
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

    const searchCriteria: EncoursFactoringSearchDto = this.searchForm.value;

    this.factoringService
      .getEncoursForRemboursement(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursFactoringDto) => {
          this.factoringList = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
        },
        error: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Failed to load outstanding factoring records.',
          });
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
