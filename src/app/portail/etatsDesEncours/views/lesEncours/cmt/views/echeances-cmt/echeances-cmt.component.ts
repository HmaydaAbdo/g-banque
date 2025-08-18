import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { CommonModule, DatePipe } from '@angular/common';
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
import { PeriodeEncoursCMT } from '../../dtos/PeriodeEncoursCMT';
import {EncoursCMTResponseDTO} from "../../dtos/EncoursCMTResponseDTO";
import {EcheanceCmtService} from "../../../../../../cmt/services/EcheanceCmtService";
import {EncoursCMTRequestDTO} from "../../dtos/EncoursCMTRequestDTO";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";
import {InputTextareaModule} from "primeng/inputtextarea";
import {RapprochemntService} from "../../../../../rapprochement/services/rapprochemnt.service";
import {EcheanceLeasingResponse} from "../../../../../../leasing/contratsLeasing/dtos/EcheanceLeasingResponse";
import {
  DonneesComplementairesRapprochementDTO
} from "../../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {LeasingTransactionRequest} from "../../../../../rapprochement/dtos/LeasingTransactionRequest";
import {TransactionSource} from "../../../../../rapprochement/dtos/TransactionSource";
import {CmtTransactionRequest} from "../../../../../rapprochement/dtos/CmtTransactionRequest";
import {EcheanceCMTResponseDto} from "../../../../../../cmt/dtos/EcheanceCMTResponseDto";


@Component({
  selector: 'app-echeances-cmt',
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
    DatePipe, // <-- This is okay to have here, but it's redundant since it's in CommonModule.
    CalendarModule,
    DialogModule,
    InputTextareaModule
  ],
  templateUrl: './echeances-cmt.component.html',
  styleUrl: './echeances-cmt.component.scss',
  providers: [MessageService, ConfirmationService] // Removed DatePipe from here
})
export class EcheancesCmtComponent implements OnInit, OnDestroy {

  encoursResponse: EncoursCMTResponseDTO | null = null;
  echeances: EcheanceCMTResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;

  searchForm!: FormGroup;
  banqueIdFromRoute!: number;

  periodeOptions: { label: string, value: PeriodeEncoursCMT }[] = [
    { label: 'Jour', value: PeriodeEncoursCMT.JOUR },
    { label: 'Mois', value: PeriodeEncoursCMT.MOIS }
  ];

  // Propriétés pour la modale de rapprochement
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedEcheance: EcheanceCMTResponseDto | null = null;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private echeanceCmtService: EcheanceCmtService,
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService,
    private fb: FormBuilder,
    private confirmationService: ConfirmationService,
    private rapprochementService: RapprochemntService,
    private datePipe: DatePipe,
  ) { }

  ngOnInit(): void {
    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          this.initSearchForm();
          this.onSearch();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      periode: [PeriodeEncoursCMT.MOIS]
    });

    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.onSearch();
      });
    this.subscriptions.push(formValueChangesSub);
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: [''],
      // Ajout des validateurs pour les pourcentages de TVA
      tvaCollectee: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      tvaDeductible: [0, [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  loadEncours(event: TableLazyLoadEvent): void {
    const searchCriteria = this.searchForm.value;

    if (!searchCriteria.periode) {
      this.messageService.add({ severity: 'info', summary: 'Information', detail: 'Veuillez sélectionner une période pour lancer la recherche.' });
      this.echeances = [];
      return;
    }

    this.loading = true;

    const request: EncoursCMTRequestDTO = {
      banqueId: this.banqueIdFromRoute,
      periode: searchCriteria.periode,
      page: event.first! / event.rows!,
      size: event.rows!,
      sort: event.sortField && event.sortOrder
        ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
        : 'dateEcheance,asc'
    };

    this.echeanceCmtService.getEncoursCMT(request).subscribe({
      next: (response: EncoursCMTResponseDTO) => {
        this.encoursResponse = response;
        this.echeances = response.echeances.content;
        this.totalRecords = response.echeances.page.totalElements;
        this.loading = false;
        this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');

      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des encours CMT:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des encours CMT.' });
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

  openRapprochementModal(echeance: EcheanceCMTResponseDto): void {
    this.selectedEcheance = echeance;
    this.initRapprochementForm(); // Assurez-vous d'initialiser le formulaire
    this.displayModal = true;
  }

  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedEcheance) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      // Create the CmtTransactionRequest object
      const cmtTransactionRequest: CmtTransactionRequest = {
        cmtId: this.selectedEcheance.credit.id,
        echeanceId: this.selectedEcheance.id,
        transactionSource: TransactionSource.CMT,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaCollectee: donneesComplementaires.tvaCollectee,
        tvaDeductible: donneesComplementaires.tvaDeductible,
      };

      this.rapprochementService.echeanceCmtToRapprochement(cmtTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Échéance CMT rapprochée avec succès.'
            });
            this.onSearch(); // Refresh the table
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement CMT:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message || 'Une erreur est survenue lors du rapprochement.'
            });
          }
        });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs de saisie.'
      });
    }
  }
}
