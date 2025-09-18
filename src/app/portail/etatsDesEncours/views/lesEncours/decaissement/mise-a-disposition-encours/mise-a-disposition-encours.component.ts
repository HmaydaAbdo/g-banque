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
import {RippleModule} from 'primeng/ripple';
import {MiseADispositionResponseDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionResponseDto";
import {MiseADispositionService} from "../../../../../Mise-à-disposition/service/MiseADispositionService";
import {PageResponse} from "../../../../../global/types/PageResponse";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {
  MiseAdispositionTransactionRequestDto
} from "../../../../rapprochement/dtos/mise_a_disposition/MiseAdispositionTransactionRequestDto";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {
  DonneesComplementairesRapprochementDTO
} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {DividerModule} from "primeng/divider";
import {EncoursMiseAdisposition} from "../../../../../Mise-à-disposition/dtos/EncoursMiseAdisposition";
import {Decimal} from "decimal.js";
import {MiseADispositionSearchDto} from "../../../../../Mise-à-disposition/dtos/MiseADispositionSearchDto";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";


@Component({
  selector: 'app-mise-a-disposition-encours',
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
  ],
  templateUrl: './mise-a-disposition-encours.component.html',
  styleUrl: './mise-a-disposition-encours.component.scss',
  providers: [MessageService],
})
export class MiseADispositionEncoursComponent implements OnInit {
  misesADisposition: MiseADispositionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: Decimal;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Properties for the reconciliation modal
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedMiseADisposition: MiseADispositionResponseDto | null = null;

  // New properties for search and TVA
  searchForm!: FormGroup;
  currentSearchDto: MiseADispositionSearchDto = {};
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];


  constructor(
    private miseADispositionService: MiseADispositionService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
  ) {
  }

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingMisesADisposition();
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

    // Subscribe to form value changes with debounce
    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: MiseADispositionSearchDto) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingMisesADisposition();
      });
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tvaDeductible: [0, Validators.required]
    });
  }

  openRapprochementModal(miseADisposition: MiseADispositionResponseDto): void {
    this.selectedMiseADisposition = miseADisposition;
    this.initRapprochementForm();
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

    if (this.rapprochementForm.valid && this.selectedMiseADisposition) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      const miseADispositionTransactionRequest: MiseAdispositionTransactionRequestDto = {
        numeroMiseAdisposition: this.selectedMiseADisposition.numero,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.MiseADisposition
      };

      this.rapprochementService.miseAdispoToRapprochement(miseADispositionTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Mise à disposition rapprochée avec succès.'
            });
            this.loadOutstandingMisesADisposition();
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement de la mise à disposition:', error);
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

  /**
   * Loads the outstanding mise à dispositions for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingMisesADisposition(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.miseADispositionService
      .getOutstandingMisesADispositionByBank(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursMiseAdisposition) => {
          this.misesADisposition = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = response.totalDesEncours;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des mises à disposition en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des mises à disposition en cours.',
          });
        },
      });
  }
}
