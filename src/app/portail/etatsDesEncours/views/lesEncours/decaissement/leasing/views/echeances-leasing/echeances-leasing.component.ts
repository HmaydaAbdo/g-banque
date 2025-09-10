import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
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
import { DialogModule } from 'primeng/dialog'; // Import du module de dialogue
import { CalendarModule } from 'primeng/calendar'; // Import du module de calendrier
import { InputNumberModule } from 'primeng/inputnumber'; // Import du module d'input number
import { InputTextareaModule } from 'primeng/inputtextarea'; // Import du module de textarea
import { Decimal } from "decimal.js"; // Import Decimal.js
import { EcheanceLeasingResponse } from "../../../../../../../leasing/contratsLeasing/dtos/EcheanceLeasingResponse";
import { EncoursLeasingResponseDTO } from "../../dtos/encours-leasing-response.dto";
import { PeriodeEncours } from "../../dtos/PeriodeEncours";
import { EcheanceLeasingService } from "../../../../../../../leasing/contratsLeasing/services/EcheanceLeasingService";
import { OrganismeLeasingService } from "../../../../../../../leasing/organismesLeasing/services/organisme-leasing.service";
import { PageResponse } from "../../../../../../../global/types/PageResponse";
import { OrganismeLeasingResponse } from "../../../../../../../leasing/organismesLeasing/dtos/OrganismeLeasingResponse";
import { EncoursLeasingRequestDTO } from "../../dtos/encours-leasing-request.dto";
import { RapprochemntService } from "../../../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import { DonneesComplementairesRapprochementDTO } from "../../../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {LeasingTransactionRequest} from "../../../../../../rapprochement/dtos/leasing/LeasingTransactionRequest";
import {TransactionSource} from "../../../../../../rapprochement/dtos/transactions/TransactionSource";


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
    TableModule,
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
    InputTextareaModule
  ],
  templateUrl: './echeances-leasing.component.html',
  styleUrl: './echeances-leasing.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class EcheancesLeasingComponent implements OnInit, OnDestroy {

  encoursResponse: EncoursLeasingResponseDTO | null = null;
  echeances: EcheanceLeasingResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  banqueIdFromRoute!: number; // Propriété pour stocker l'ID de la banque


  organismeLeasingOptions: { label: string, value: number }[] = [];

  // Propriétés pour la modale de rapprochement
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedEcheance: EcheanceLeasingResponse | null = null;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private echeanceLeasingService: EcheanceLeasingService,
    private organismeLeasingService: OrganismeLeasingService,
    private rapprochementService: RapprochemntService, // Injection du service
    private route: ActivatedRoute, // Injection d'ActivatedRoute
    private router: Router,
    private messageService: MessageService,
    private fb: FormBuilder,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe
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
          this.onSearch(); // Lancer la recherche initiale avec le bon ID
        }
      })
    );

    // Initialisation du formulaire de recherche (assurez-vous qu'il est initialisé avant de s'y abonner)
    this.initSearchForm();
    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.onSearch();
      });
    this.subscriptions.push(formValueChangesSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    // Supprimez `banqueId` du formulaire
    this.searchForm = this.fb.group({
      periode: [PeriodeEncours.MOIS],
      organismeLeasingId: [null]
    });
  }

  // Nouvelle méthode pour initialiser le formulaire de rapprochement
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['',Validators.required],
      tvaDeductible: [0, [Validators.required, Validators.min(0), Validators.max(100)]]
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
    // Récupérez les critères de recherche SANS le champ `banqueId`
    const searchCriteria = this.searchForm.value as Omit<EncoursLeasingRequestDTO, 'banqueId'>;

    if (!searchCriteria.periode) {
      this.messageService.add({ severity: 'info', summary: 'Information', detail: 'Veuillez sélectionner une période pour lancer la recherche.' });
      this.echeances = [];
      return;
    }

    this.loading = true;

    // Créez l'objet de requête en combinant l'ID de la route et les critères du formulaire
    const request: EncoursLeasingRequestDTO = {
      banqueId: this.banqueIdFromRoute, // Utilisation de l'ID de la route
      ...searchCriteria,
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

  // Nouvelle méthode pour ouvrir la modale
  openRapprochementModal(echeance: EcheanceLeasingResponse): void {
    this.selectedEcheance = echeance;
    this.initRapprochementForm(); // Assurez-vous d'initialiser le formulaire
    this.displayModal = true;
  }

  // Nouvelle méthode pour soumettre les données de rapprochement
  submitRapprochement(): void {
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
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      // Create the LeasingTransactionRequest object
      const leasingTransactionRequest: LeasingTransactionRequest = {
        contratLeasingId: this.selectedEcheance.contratLeasingId  ,
        echeanceId: this.selectedEcheance.id,
        transactionSource: TransactionSource.LEASING,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
      };

      this.rapprochementService.echeanceLeasingToRapprochemnt(leasingTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response)
            this.onSearch()
          },
          error:(error) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message
            });
          },

        })
      this.messageService.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Échéance rapprochée avec succès.'
      });

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
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  protected readonly Date = Date;
}
