import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule, DatePipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {MessageService} from 'primeng/api';
import {ChequeResponseDto} from "../../../../../cheques/dtos/ChequeResponseDto";
import {ChequeService} from "../../../../../cheques/services/ChequeService";
import {Ripple} from "primeng/ripple";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {DonneesComplementairesRapprochementDTO} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {ChequeTransactionRequest} from "../../../../rapprochement/dtos/cheques/ChequeTransactionRequest";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {DividerModule} from "primeng/divider";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {EncoursChequeResponseDto} from "../../../../../cheques/dtos/EncoursChequeResponseDto";
import {Decimal} from "decimal.js";
import {EncoursChequeSearchDto} from "../../../../../cheques/dtos/EncoursChequeSearchDto";
import {InputTextModule} from "primeng/inputtext";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";

@Component({
  selector: 'app-cheques-encours',
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
    Ripple,
    DividerModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
  ],
  templateUrl: './cheques-encours.component.html',
  styleUrl: './cheques-encours.component.scss',
  providers: [MessageService],
})
export class ChequesEncoursComponent implements OnInit {
  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: Decimal;
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


  constructor(
    private chequeService: ChequeService,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe
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
      beneficiaire: ['']
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

    if (this.rapprochementForm.valid && this.selectedCheque) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      const chequeTransactionRequest: ChequeTransactionRequest = {
        numeroCheque: this.selectedCheque.numero,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.CHEQUE,
        libelle:donneesComplementaires.libelle
      };

      this.rapprochementService.chequeToRapprochement(chequeTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Chèque rapproché avec succès.'
            });
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
      .getOutstandingChequesByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
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
