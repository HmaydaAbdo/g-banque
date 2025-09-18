import {Component, OnInit, ViewChild, OnDestroy} from '@angular/core';
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {MessageService} from "primeng/api";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {DecompteResponseDto} from "../../../../../decomptes/dtos/DecompteResponseDto";
import {DecompteService} from "../../../../../decomptes/service/DecompteService";
import {DecompteTransactionRequest} from "../../../../rapprochement/dtos/decomptes/DecompteTransactionRequest";
import {EncoursDecompteDto} from "../../../../../decomptes/dtos/EncoursDecompteDto";
import {Decimal} from "decimal.js";
import {ButtonModule} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {DatePipe, NgIf} from "@angular/common";
import {DialogModule} from "primeng/dialog";
import {RippleModule} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {InputTextModule} from "primeng/inputtext";
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { EncoursDecompteSearchDto } from "../../../../../decomptes/dtos/EncoursDecompteSearchDto";
import {DropdownModule} from "primeng/dropdown";


@Component({
  selector: 'app-decomptes',
  standalone: true,
  imports: [
    ButtonModule,
    CalendarModule,
    DatePipe,
    DialogModule,
    NgIf,
    ReactiveFormsModule,
    RippleModule,
    TableModule,
    ToastModule,
    TooltipModule,
    InputTextModule,
    DropdownModule
  ],
  templateUrl: './decomptes.component.html',
  styleUrl: './decomptes.component.scss',
  providers: [MessageService]
})
export class DecomptesComponent implements OnInit, OnDestroy {

  decomptes: DecompteResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueId: number = 1;
  totalEncours!: Decimal;
  @ViewChild('dt') dt: Table | undefined;

  // New properties for search and TVA
  searchForm!: FormGroup;
  private searchSubscription!: Subscription;
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  //  properties for the dialog and form
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedDecompte: DecompteResponseDto | null = null;

  constructor(
    private decompteService: DecompteService,
    private messageService: MessageService,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initSearchForm();
    this.initRapprochementForm();

    this.searchSubscription = this.searchForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        if (this.dt) {
          this.dt.first = 0;
          this.loadEncoursDecomptes();
        }
      });
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
    });
    this.loadEncoursDecomptes();
  }

  /**
   * Clears the search form and reloads the table.
   */
  clearSearch(): void {
    this.searchForm.reset();
    if (this.dt) {
      this.dt.first = 0;
      this.loadEncoursDecomptes();
    }
  }

  /**
   * Initializes the reactive form for the rapprochement data.
   */
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tva: [null, Validators.required],
    });
  }

  /**
   * Opens the dialog to validate a decompte.
   * @param decompte The decompte record to validate.
   */
  openRapprochementModal(decompte: DecompteResponseDto): void {
    this.selectedDecompte = decompte;
    this.initRapprochementForm();
    this.displayModal = true;
  }

  /**
   * Handles the form submission to send the rapprochement request.
   */
  submitRapprochement(): void {
    if (this.rapprochementForm.invalid || !this.selectedDecompte) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires.',
      });
      return;
    }

    // Construct the request DTO using the selected dc and form data
    const decompteTransactionRequest:DecompteTransactionRequest= {
      decompteId: this.selectedDecompte.id,
      banqueId: this.selectedDecompte.banqueId,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateValeur: this.rapprochementForm.value.dateValeur,
      tva: this.rapprochementForm.value.tva
    };

    this.rapprochementService.decompteToRapprochement(decompteTransactionRequest)
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'décompte  validée et rapprochée avec succès.',
          });
          this.loadEncoursDecomptes();
          this.displayModal = false;
        },
        error: (error) => {
          console.error('Erreur lors du rapprochement du décompte:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error?.message || 'Une erreur est survenue lors du rapprochement.',
          });
        },
      });
  }

  /**
   * Loads outstanding dc disbursement records.
   * Handles pagination and sorting from the PrimeNG table.
   */
  loadEncoursDecomptes(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursDecompteSearchDto = this.searchForm.value;

    this.decompteService
      .getEncoursDecomptes(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursDecompteDto) => {
          this.decomptes=response.page.content
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
        },
        error: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des encaissements des décomptes.',
          });
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
