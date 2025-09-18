import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';
import { RippleModule } from 'primeng/ripple';
import { MessageService } from 'primeng/api';
import { PrefinancementResponseDTO } from "../../../../../prefinancements/dtos/PrefinancementResponseDTO";
import { PrefinancementService } from "../../../../../prefinancements/service/PrefinancementService";
import { EncoursPrefinancementDto } from "../../../../../prefinancements/dtos/EncoursPrefinancementDto";
import { Decimal } from "decimal.js";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { RapprochemntService } from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import { PrefinancementTransactionRequest } from "../../../../rapprochement/dtos/prefinancement/PrefinancementTransactionRequest";
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";
import { InputTextModule } from 'primeng/inputtext';
import { EncoursPrefinancementSearchDto } from "../../../../../prefinancements/dtos/EncoursPrefinancementSearchDto";
import { DropdownModule } from 'primeng/dropdown';

import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-prefinancement-deblocage',
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
    InputTextModule,
    DropdownModule,
  ],
  templateUrl: './prefinancement-deblocage.component.html',
  styleUrl: './prefinancement-deblocage.component.scss',
  providers: [MessageService],
})
export class PrefinancementDeblocageComponent implements OnInit, OnDestroy {
  prefinancements: PrefinancementResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueId: number = 1;
  totalEncours!: Decimal;
  @ViewChild('dt') dt: Table | undefined;

  // Search form properties
  searchForm!: FormGroup;
  private searchSubscription!: Subscription;

  // properties for the dialog and form
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedPrefinancement: PrefinancementResponseDTO | null = null;

  // TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  constructor(
    private prefinancementService: PrefinancementService,
    private messageService: MessageService,
    private datePipe: DatePipe,
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
          this.loadEncoursDeblocages();
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
    this.loadEncoursDeblocages();
  }

  /**
   * Initializes the reactive form for the rapprochement data.
   */
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tvaCollecte: [null, Validators.required],
    });
  }

  /**
   * Opens the dialog to validate a prefinancement.
   * @param prefinancement The prefinancement record to validate.
   */
  openRapprochementModal(prefinancement: PrefinancementResponseDTO): void {
    this.selectedPrefinancement = prefinancement;
    this.initRapprochementForm();
    this.displayModal = true;
  }

  /**
   * Handles the form submission to send the rapprochement request.
   */
  submitRapprochement(): void {
    if (this.rapprochementForm.invalid || !this.selectedPrefinancement) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires.',
      });
      return;
    }

    const prefinancementTransactionRequest: PrefinancementTransactionRequest = {
      prefinancementId: this.selectedPrefinancement.id,
      banqueId: this.selectedPrefinancement.banqueId,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateValeur: this.rapprochementForm.value.dateValeur,
      tva: this.rapprochementForm.value.tvaCollecte,
    };

    this.rapprochementService.prefinancementToRapprochement(prefinancementTransactionRequest)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Préfinancement validé et rapproché avec succès.',
          });
          this.loadEncoursDeblocages();
          this.displayModal = false;
        },
        error: (error) => {
          console.error('Erreur lors du rapprochement du préfinancement:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error?.message || 'Une erreur est survenue lors du rapprochement.',
          });
        },
      });
  }

  /**
   * Clears the search form and reloads the table.
   */
  clearSearch(): void {
    this.searchForm.reset();
    if (this.dt) {
      this.dt.first = 0;
      this.loadEncoursDeblocages();
    }
  }

  /**
   * Loads outstanding pre-financing disbursement records.
   */
  loadEncoursDeblocages(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursPrefinancementSearchDto = this.searchForm.value;

    this.prefinancementService
      .getEncoursDeblocage(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursPrefinancementDto) => {
          this.prefinancements = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
        },
        error: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Failed to load outstanding pre-financing disbursement records.',
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
