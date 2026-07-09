import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';
import { RippleModule } from 'primeng/ripple';
import {ConfirmationService, MessageService} from 'primeng/api';
import { DroitConstateResponseDto } from "../../../../../droitsConstate/dtos/DroitConstateResponseDto";
import { DroitConstateService } from "../../../../../droitsConstate/service/DroitConstateService";
import { EncoursDroitConstateDto } from "../../../../../droitsConstate/dtos/EncoursDroitConstateDto";
import { DroitConstateTransactionRequest } from "../../../../dtos/droits_constate/DroitConstateTransactionRequest";
import { ValidationService } from "../../../../services/validation/validation.service";
import { Decimal } from "decimal.js";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import {  Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { EncoursDroitConstateSearchDto } from "../../../../../droitsConstate/dtos/EncoursDroitConstateSearchDto";
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";
import {ActivatedRoute, RouterLink} from '@angular/router';
import {InputTextareaModule} from "primeng/inputtextarea";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";


@Component({
  selector: 'app-droit-constate-remboursements',
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
  templateUrl: './droit-constate-remboursements.component.html',
  styleUrl: './droit-constate-remboursements.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class DroitConstateRemboursementsComponent implements OnInit, OnDestroy {
  droitConstates: DroitConstateResponseDto[] = [];
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
  selectedDroitConstate: DroitConstateResponseDto | null = null;

  // New TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  isSubmitting: boolean = false;

  // Validation en lot
  selectionState = new TableSelectionState<DroitConstateResponseDto>(d => d.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): DroitConstateResponseDto[] {
    return this.selectionState.getPageSelection(this.droitConstates);
  }

  onSelectionChange(selection: DroitConstateResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.droitConstates, selection);
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
    this.dt?.reset();
    this.bankStateService.refreshBankInfo(this.banqueId);
    this.bankStateService.notifyValidation();
  }

  constructor(
    private droitConstateService: DroitConstateService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private rapprochementService: ValidationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private fb: FormBuilder,
    private route: ActivatedRoute,
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

  /**
   * Initializes the reactive form for the search criteria.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      marcheNumero: [''],
      marcheName: [''],
      montant:[null]
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

  /**
   * Initializes the reactive form for the validation data.
   */
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

  /**
   * Opens the dialog to validate a dc.
   * @param dc The droit constaté record to validate.
   */
  openRapprochementModal(dc: DroitConstateResponseDto): void {
    this.selectedDroitConstate = dc;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(dc.dateEmission)!,
      libelle: dc.libelle,
      observation : dc.observation
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

    if (this.rapprochementForm.valid && this.selectedDroitConstate) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedDroitConstate = this.selectedDroitConstate;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce Droit Constaté ?
            <br><br>
            <strong>N° de Marché:</strong> ${selectedDroitConstate.marcheNumero}
            <br>
            <strong>Montant du DC:</strong> ${this.decimalPipe.transform(selectedDroitConstate.montant,"1.2-2",'fr-FR')} MAD
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
        header: 'Confirmation de Rapprochement Droit Constaté',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement(); // Appel à la méthode d'exécution
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
    if ( this.selectedDroitConstate &&  this.rapprochementForm.valid ) {


      this.isSubmitting = true;
      const rapprochementId = this.bankStateService.getRapprochementId();
      const dcTransactionRequest: DroitConstateTransactionRequest = {
        droitConstateId: this.selectedDroitConstate.id,
        banqueId: this.selectedDroitConstate.banqueId,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateOperation:this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        tva: this.rapprochementForm.value.tva,
        libelle: this.rapprochementForm.value.libelle,
        observation: this.rapprochementForm.value.observation,
        rapprochementId: rapprochementId!
      };


      this.rapprochementService.droitConstateToRapprochement(dcTransactionRequest)
        .subscribe({
          next: (response) => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Droit constaté validé et rapproché avec succès.',
            });
            this.dt?.reset();
            this.isSubmitting = false;
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueId);
            this.bankStateService.notifyValidation();

          },
          error: (error) => {
            console.error('Erreur lors du validation du droit constaté:', error);
            this.isSubmitting = false;
          },
        });
    }
    }



  /**
   * Loads outstanding dc repayment records with search criteria.
   */
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

    const searchCriteria: EncoursDroitConstateSearchDto = this.searchForm.value;

    this.droitConstateService
      .getEncoursRemboursement(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursDroitConstateDto) => {
          this.droitConstates = response.page.content;
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
