import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { CommonModule, DatePipe } from '@angular/common';
import { ConfirmationService, MessageService, SelectItem } from 'primeng/api'; // Import SelectItem
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

import {EncoursCMTResponseDTO} from "../../dtos/EncoursCMTResponseDTO";
import {EcheanceCmtService} from "../../../../../../../cmt/services/EcheanceCmtService";
import {EncoursCMTRequestDTO} from "../../dtos/EncoursCMTRequestDTO";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";
import {InputTextareaModule} from "primeng/inputtextarea";
import {RapprochemntService} from "../../../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {DonneesComplementairesRapprochementDTO} from "../../../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {TransactionSource} from "../../../../../../rapprochement/dtos/transactions/TransactionSource";
import {CmtTransactionRequest} from "../../../../../../rapprochement/dtos/cmt/CmtTransactionRequest";
import {EcheanceCMTResponseDto} from "../../../../../../../cmt/dtos/EcheanceCMTResponseDto";
import { InputTextModule } from 'primeng/inputtext';


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
    DatePipe,
    CalendarModule,
    DialogModule,
    InputTextareaModule,
    InputTextModule
  ],
  templateUrl: './echeances-cmt.component.html',
  styleUrl: './echeances-cmt.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class EcheancesCmtComponent implements OnInit, OnDestroy {

  encoursResponse: EncoursCMTResponseDTO | null = null;
  echeances: EcheanceCMTResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;


  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  searchForm!: FormGroup;
  banqueIdFromRoute!: number;

  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedEcheance: EcheanceCMTResponseDto | null = null;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private echeanceCmtService: EcheanceCmtService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private fb: FormBuilder,
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
      designation: [''] // Add the new form control
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
      observation: ['',Validators.required],
      tvaDeductible: [null, Validators.required] ,
      libelle: ['', Validators.required]
    });
  }

  loadEncours(event: TableLazyLoadEvent): void {
    const searchCriteria = this.searchForm.value;


    this.loading = true;

    const request: EncoursCMTRequestDTO = {
      banqueId: this.banqueIdFromRoute,
      designation: searchCriteria.designation, // Pass the designation to the request
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
        this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
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
    this.initRapprochementForm();
    this.selectedEcheance = echeance;
    this.initRapprochementForm();

    const creditTaux = echeance.credit.taux;
    // Ensure the dynamic rate is available in tvaOptions for the dropdown to display it
    const isTauxPresent = this.tvaOptions.some(opt => opt.value === creditTaux);
    if (!isTauxPresent) {
      // Add the specific credit taux  to the options list temporarily
      this.tvaOptions.push({ label: `${creditTaux.toFixed(2)}%`, value: creditTaux });
    }

    this.rapprochementForm.patchValue({
      // Pre-fill dateEmission with dateEcheance (convert string to Date object)
      dateEmission: new Date(echeance.dateEcheance),

      // Pre-fill TVA with the credit's rate (taux)
      tvaDeductible: creditTaux,

      // Pre-fill modifiable text fields
      observation: echeance.observation,
      libelle: echeance.libelle,

      // dateValeur and dateOperation remain null for user entry
    });

    // 2. Disabling controls
    this.rapprochementForm.get('dateEmission')?.disable();
    this.rapprochementForm.get('tvaDeductible')?.disable();
    this.displayModal = true;
  }

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
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.getRawValue() as DonneesComplementairesRapprochementDTO;

      const cmtTransactionRequest: CmtTransactionRequest = {
        cmtId: this.selectedEcheance.credit.id,
        echeanceId: this.selectedEcheance.id,
        transactionSource: TransactionSource.CMT,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        libelle:donneesComplementaires.libelle,
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

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
