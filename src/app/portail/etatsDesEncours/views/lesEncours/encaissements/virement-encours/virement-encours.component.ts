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
import {VirementResponseDto} from "../../../../../Virements/dtos/VirementResponseDto";
import {VirementService} from "../../../../../Virements/service/VirementService";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {VirementTransactionRequestDto} from "../../../../rapprochement/dtos/virements/VirementTransactionRequestDto";
import {DonneesComplementairesRapprochementDTO} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {DividerModule} from "primeng/divider";
import {InputTextareaModule} from "primeng/inputtextarea";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";
import {EncoursVirementResponseDto} from "../../../../../Virements/dtos/EncoursVirementResponseDto";
import {Decimal} from "decimal.js";
import {EncoursVirementsSearchDto} from "../../../../../Virements/dtos/EncoursVirementsSearchDto";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {ConfirmDialogModule} from "primeng/confirmdialog";


@Component({
  selector: 'app-virement-encaissement-encours',
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
    DividerModule,
    InputTextareaModule,
    CalendarModule,
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ConfirmDialogModule,
  ],
  templateUrl: './virement-encours.component.html',
  providers: [MessageService,ConfirmationService,DecimalPipe],
})
export class VirementEncaissementEncoursComponent implements OnInit {
  virements: VirementResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Propriétés pour la modale de rapprochement
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedVirement: VirementResponseDto | null = null;

  // New properties for search and TVA
  searchForm!: FormGroup;
  currentSearchDto: EncoursVirementsSearchDto = {};
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  constructor(
    private virementService: VirementService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private rapprochementService: RapprochemntService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
  ) {
  }

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingVirements();
      }
    });
    this.initSearchForm();
    this.initRapprochementForm();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numero: [''],
      beneficiaire: [''],
      montant: [null],
    });

    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: EncoursVirementsSearchDto) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingVirements();
      });
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tvaDeductible: [null, Validators.required],
      libelle: ['', Validators.required],
    });
  }

  openRapprochementModal(virement: VirementResponseDto): void {
    this.selectedVirement = virement;
    this.initRapprochementForm();

    this.rapprochementForm.patchValue({
      dateEmission: new Date(virement.dateEmission),
      libelle:virement.libelle,
      observation:virement.observation
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

    if (this.rapprochementForm.valid && this.selectedVirement) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedVirement = this.selectedVirement;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce virement ?
            <br><br>
            <strong>N° Virement:</strong> ${selectedVirement.numero}
            <br>
            <strong>Bénéficiaire:</strong> ${selectedVirement.beneficiaire}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedVirement.montant,'1.2-2',"fr-FR")} MAD
            <br>
            <strong>Type:</strong> ${selectedVirement.type}
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Collecté:</strong> ${formValue.tvaDeductible}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Virement',
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


    if (this.rapprochementForm.valid && this.selectedVirement) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      const virementTransactionRequest: VirementTransactionRequestDto = {
        numeroVirement: this.selectedVirement.numero,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.Virement,
        libelle:donneesComplementaires.libelle,
      };

      this.isSubmitting = true;
      this.rapprochementService.virementEncaissementToRapprochement(virementTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Virement rapproché avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingVirements();
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement du virement:', error);
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

  /**
   * Loads the outstanding virements for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingVirements(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.virementService
      .getEncoursEncaissementVirementsByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursVirementResponseDto) => {
          this.virements = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des virements en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des virements en cours.',
          });
        },
      });
  }
}
