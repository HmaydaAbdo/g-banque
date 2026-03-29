import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {ConfirmationService, MessageService} from 'primeng/api';
import {RippleModule} from 'primeng/ripple';
import {LcnResponseDTO} from "../../../../../LCN/dtos/LcnResponseDTO";
import {LcnService} from "../../../../../LCN/service/LcnService";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ValidationService} from "../../../../services/validation/validation.service";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {LcnTransactionRequest} from "../../../../dtos/lcn/LcnTransactionRequest";
import {DonneesComplementairesRapprochementDTO} from "../../../../dtos/DonneesComplementairesRapprochementDTO";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {DividerModule} from "primeng/divider";
import {EncoursLcnResponse} from "../../../../../LCN/dtos/EncoursLcnResponse";
import {LcnSearchDTO} from "../../../../../LCN/dtos/LcnSearchDTO"; // Import the DTO
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';


@Component({
  selector: 'app-lcn-encours',
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
    InputTextareaModule,
    DividerModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ConfirmDialogModule,
  ],
  templateUrl: './lcn-encours.component.html',
  styleUrl: './lcn-encours.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class LcnEncoursComponent implements OnInit {
  lcns: LcnResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;


  // Propriétés pour la modale de validation
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedLcn: LcnResponseDTO | null = null;

  // New properties for search and TVA
  searchForm!: FormGroup;
  currentSearchDto: LcnSearchDTO = {};
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;


  constructor(
    private lcnService: LcnService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private fb: FormBuilder,
    private rapprochementService: ValidationService,
    private bankStateService: BankStateService
  ) {
  }

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingLcns();
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

    // Subscribe to form value changes with debounce
    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: LcnSearchDTO) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingLcns();
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

  openRapprochementModal(lcn: LcnResponseDTO): void {
    this.selectedLcn = lcn;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(lcn.dateEcheance)!,
      libelle:lcn.libelle,
      observation:lcn.observation,
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

    if (this.rapprochementForm.valid && this.selectedLcn) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés
      const formValue = this.rapprochementForm.getRawValue();
      const selectedLcn = this.selectedLcn;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce LCN ?
            <br><br>
            <strong>N° LCN:</strong> ${selectedLcn.numero}
            <br>
            <strong>Bénéficiaire:</strong> ${selectedLcn.beneficiaire}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedLcn.montant,"1.2-2","fr-FR")} MAD
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

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement LCN',
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



    if (this.rapprochementForm.valid && this.selectedLcn) {
      const lcnTransactionRequest: LcnTransactionRequest = {
        id: this.selectedLcn.id,
        dateEmission:this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        dateOperation:this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        tvaDeductible: this.rapprochementForm.value.tvaDeductible,
        transactionSource: TransactionSource.LCN,
        libelle:this.rapprochementForm.value.libelle
      };

      this.isSubmitting = true;

      this.rapprochementService.lcnDecaissementToRapprochement(lcnTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'LCN rapproché avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingLcns();
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);

          },
          error: (error) => {
            console.error('Erreur lors du validation du LCN:', error);
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
   * Loads the outstanding LCNs for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingLcns(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEcheance,desc';

    this.lcnService
      .encoursDecaissementLcnsByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursLcnResponse) => {
          this.lcns = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalEncours
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des LCNs en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des LCNs en cours.',
          });
        },
      });
  }
}
