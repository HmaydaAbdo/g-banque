import {Component, OnInit, ViewChild} from '@angular/core';
import {ChequeResponseDto} from "../../../../../cheques/dtos/ChequeResponseDto";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {EncoursChequeSearchDto} from "../../../../../cheques/dtos/EncoursChequeSearchDto";
import {ChequeService} from "../../../../../cheques/services/ChequeService";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {ActivatedRoute} from "@angular/router";
import {ConfirmationService, MessageService, PrimeTemplate} from "primeng/api";
import {DatePipe, DecimalPipe, NgIf} from "@angular/common";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {DonneesComplementairesRapprochementDTO} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {ChequeTransactionRequest} from "../../../../rapprochement/dtos/cheques/ChequeTransactionRequest";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {EncoursChequeResponseDto} from "../../../../../cheques/dtos/EncoursChequeResponseDto";
import {Button} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DialogModule} from "primeng/dialog";
import {DividerModule} from "primeng/divider";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {InputTextareaModule} from "primeng/inputtextarea";
import {PaginatorModule} from "primeng/paginator";
import {Ripple} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'app-cheque-encaissements-encours',
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
    InputTextModule,
    InputTextareaModule,
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    TableModule,
    ToastModule,
    TooltipModule
  ],
  templateUrl: './cheque-encaissements-encours.component.html',
  styleUrl: './cheque-encaissements-encours.component.scss',
  providers: [MessageService,ConfirmationService,DecimalPipe],

})
export class ChequeEncaissementsEncoursComponent implements OnInit {
  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // New properties for search
  searchForm!: FormGroup;
  currentSearchDto: EncoursChequeSearchDto = {};

  // Properties for the rapprochement modal
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

  constructor(
    private chequeService: ChequeService,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private ConfirmationService:ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe
  ) {}

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
      dateEmission: new Date(cheque.dateEmission) ,
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
            <strong>Id Chèque:</strong> ${selectedCheque.id}
            <br>
            <strong>Bénéficiaire:</strong> ${selectedCheque.beneficiaire}
            <br>
            <strong>Emetteur:</strong> ${selectedCheque.emetteur}
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

      const chequeTransactionRequest: ChequeTransactionRequest = {
        idCheque: this.selectedCheque.id,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.CHEQUE,
        libelle:donneesComplementaires.libelle
      };

      this.isSubmitting = true;

      this.rapprochementService.chequeEncaissementToRapprochement(chequeTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Chèque rapproché avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingCheques();
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement du chèque:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message || 'Une erreur est survenue lors du rapprochement.'
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

  loadOutstandingCheques(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;

    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.chequeService
      .getEncoursEncaissementChequesByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
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
          console.error(
            'Erreur lors du chargement des chèques en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des chèques en cours.',
          });
        },
      });
  }


}
