import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';
import { PaginatorModule } from 'primeng/paginator';
import { DividerModule } from 'primeng/divider';
import { DialogModule } from 'primeng/dialog';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { EcheanceLeasingResponse } from "../../../../../leasing/contratsLeasing/dtos/EcheanceLeasingResponse";
import { EncoursLeasingResponseDTO } from "../../../../../leasing/contratsLeasing/dtos/encours-leasing-response.dto";
import { EcheanceLeasingService } from "../../../../../leasing/contratsLeasing/services/EcheanceLeasingService";
import { OrganismeLeasingService } from "../../../../../leasing/organismesLeasing/services/organisme-leasing.service";
import { PageResponse } from "../../../../../global/types/PageResponse";
import { OrganismeLeasingResponse } from "../../../../../leasing/organismesLeasing/dtos/OrganismeLeasingResponse";
import { EncoursLeasingRequestDTO } from "../../../../../leasing/contratsLeasing/dtos/encours-leasing-request.dto";
import { ValidationService } from "../../../../services/validation/validation.service";
import { DonneesComplementairesRapprochementDTO } from "../../../../dtos/DonneesComplementairesRapprochementDTO";
import {LeasingTransactionRequest} from "../../../../dtos/leasing/LeasingTransactionRequest";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';


@Component({
  selector: 'app-echeances-leasing',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    DropdownModule,
    TagModule,
    TooltipModule,
    ConfirmDialogModule,
    RippleModule,
    PaginatorModule,
    DividerModule,
    DatePipe,
    DialogModule,
    CalendarModule,
    InputNumberModule,
    InputTextareaModule,
    InputTextModule,
    TableModule,
  ],
  templateUrl: './echeances-leasing.component.html',
  styleUrl: './echeances-leasing.component.scss',
  providers: [MessageService, ConfirmationService,DecimalPipe]
})
export class EcheancesLeasingComponent implements OnInit, OnDestroy {

  encoursResponse: EncoursLeasingResponseDTO | null = null;
  echeances: EcheanceLeasingResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  banqueIdFromRoute!: number; // Propriété pour stocker l'ID de la banque

  tvaOptions = [
    { label: '0 %', value: 0 },
    { label: '10 %', value: 10 },
    { label: '14 %', value: 14 },
    { label: '20 %', value: 20 },
  ];

  organismeLeasingOptions: { label: string, value: number }[] = [];

  // Propriétés pour la modale de validation
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedEcheance: EcheanceLeasingResponse | null = null;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  isSubmitting: boolean = false;


  constructor(
    private echeanceLeasingService: EcheanceLeasingService,
    private organismeLeasingService: OrganismeLeasingService,
    private rapprochementService: ValidationService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private bankStateService: BankStateService

  ) { }

  ngOnInit(): void {
    // Récupérer l'ID de la banque depuis la route parent

    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id; // Convertir la chaîne en nombre
          this.initSearchForm();
          this.loadDropdownOptions();
        }
      })
    );

    // Initialisation du formulaire de recherche
    this.initSearchForm();

    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        // Automatically trigger search when form values change
        this.onSearch();
      });

    this.subscriptions.push(formValueChangesSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      organismeLeasingId: [null],
      numeroContrat: [null],
      montant: [null]
    });
  }

  // Nouvelle méthode pour initialiser le formulaire de validation
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required], // Ajout du champ libelle
      tvaDeductible: [20, [Validators.required, Validators.min(0), Validators.max(100)]] // Valeur par défaut 20%
    });
  }

  loadDropdownOptions(): void {
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeLeasingOptions = response.content.map(org => ({
            label: org.nom,
            value: org.id!
          }));
        },
        error: (error) => {
          console.error('Error loading organisme leasing options:', error);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des organismes de leasing.' });
        }
      });
  }

  loadEncours(event: TableLazyLoadEvent): void {
    const searchCriteria = this.searchForm.value as EncoursLeasingRequestDTO;
    this.loading = true;
    const request: EncoursLeasingRequestDTO = {
      banqueId: this.banqueIdFromRoute,
      organismeLeasingId: searchCriteria.organismeLeasingId,
      numeroContrat: searchCriteria.numeroContrat,
      montant: searchCriteria.montant,
      page: event.first! / event.rows!,
      size: event.rows!,
      sort: event.sortField && event.sortOrder
        ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
        : 'dateEcheance,asc'
    };

    this.echeanceLeasingService.getEncoursLeasing(request).subscribe({
      next: (response: EncoursLeasingResponseDTO) => {
        this.encoursResponse = response;
        this.echeances = response.echeances.content;
        this.totalRecords = response.echeances.page.totalElements;
        this.loading = false;
        this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des encours:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des encours.' });
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadEncours(this.dt.createLazyLoadMetadata());
    } else {
      this.loadEncours({
        first: 0,
        rows: 10,
        filters: {},
        sortField: 'dateEcheance',
        sortOrder: 1
      });
    }
  }


  openRapprochementModal(echeance: EcheanceLeasingResponse): void {
    this.selectedEcheance = echeance;
    this.initRapprochementForm(); // Assurez-vous d'initialiser le formulaire

    // Pré-remplissage des champs et gestion de la désactivation
    this.rapprochementForm.patchValue({
      // Pré-remplissage avec la date d'échéance et désactivation
      dateEmission: parseLocalDate(echeance.dateEcheance)!,

      // Pré-remplissage de l'observation et du libellé
      observation: echeance.observation,
      libelle: echeance.libelle,

      // Pré-remplissage de la TVA (20%) et désactivation
      tvaDeductible: 20
    });

    // Désactivation des contrôles non éditables
    this.rapprochementForm.get('dateEmission')?.disable();
    this.rapprochementForm.get('tvaDeductible')?.disable();


    // dateValeur et dateOperation restent nulls et éditables
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

    if (this.rapprochementForm.valid && this.selectedEcheance) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés
      const formValue = this.rapprochementForm.getRawValue();
      const selectedEcheance = this.selectedEcheance;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher cette échéance ?
            <br><br>
            <strong>Contrat N°:</strong> ${selectedEcheance.numeroContrat}
            <br>
            <strong>Montant TTC:</strong> ${this.decimalPipe.transform(selectedEcheance.montantTTC, "1.2-2", 'fr-FR')} MAD            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Déductible:</strong> ${this.decimalPipe.transform(formValue.tvaDeductible,"1.2-2","fr-FR")}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {

        }
      });
    }
  }

  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedEcheance) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés (dateEmission, tvaDeductible)
      const formValue = this.rapprochementForm.getRawValue();


      // Create the LeasingTransactionRequest object
      const leasingTransactionRequest: LeasingTransactionRequest = {
        contratLeasingId: this.selectedEcheance.contratLeasingId,
        echeanceId: this.selectedEcheance.id,
        transactionSource: TransactionSource.LEASING,
        dateEmission:this.datePipe.transform(formValue.dateEmission,'yyyy-MM-dd') as string ,
        dateValeur: this.datePipe.transform(formValue.dateValeur,'yyyy-MM-dd') as string,
        dateOperation:this.datePipe.transform(formValue.dateOperation,'yyyy-MM-dd') as string,
        observation:  formValue.observation,
        tvaDeductible:  formValue.tvaDeductible,
        libelle: formValue.libelle
      };

      this.isSubmitting = true;

      this.rapprochementService.echeanceLeasingToRapprochemnt(leasingTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response)
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Échéance rapprochée avec succès.'
            });
            this.isSubmitting = false;
            this.onSearch()
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
          },
          error: (error) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message
            });
            this.isSubmitting = false;
          },

        })
      this.displayModal = false;
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs de saisie.'
      });
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    // Le contrôle doit être valide pour les champs disabled
    if (control && control.disabled) {
      return false;
    }
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  protected readonly Date = Date;


}
