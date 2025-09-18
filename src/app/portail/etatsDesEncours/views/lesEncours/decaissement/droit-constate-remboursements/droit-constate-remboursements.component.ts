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
import { DroitConstateResponseDto } from "../../../../../droitsConstate/dtos/DroitConstateResponseDto";
import { DroitConstateService } from "../../../../../droitsConstate/service/DroitConstateService";
import { EncoursDroitConstateDto } from "../../../../../droitsConstate/dtos/EncoursDroitConstateDto";
import { DroitConstateTransactionRequest } from "../../../../rapprochement/dtos/droits_constate/DroitConstateTransactionRequest";
import { RapprochemntService } from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import { Decimal } from "decimal.js";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { EncoursDroitConstateSearchDto } from "../../../../../droitsConstate/dtos/EncoursDroitConstateSearchDto";
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";


@Component({
  selector: 'app-droit-constate-remboursements',
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
  templateUrl: './droit-constate-remboursements.component.html',
  styleUrl: './droit-constate-remboursements.component.scss',
  providers: [MessageService],
})
export class DroitConstateRemboursementsComponent implements OnInit, OnDestroy {
  droitConstates: DroitConstateResponseDto[] = [];
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
  selectedDroitConstate: DroitConstateResponseDto | null = null;

  // New TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  constructor(
    private droitConstateService: DroitConstateService,
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
          this.loadEncoursRemboursements();
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
    this.loadEncoursRemboursements();
  }

  /**
   * Initializes the reactive form for the rapprochement data.
   */
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tva: [null, Validators.required], // Added TVA field
    });
  }

  /**
   * Opens the dialog to validate a dc.
   * @param dc The droit constaté record to validate.
   */
  openRapprochementModal(dc: DroitConstateResponseDto): void {
    this.selectedDroitConstate = dc;
    this.initRapprochementForm();
    this.displayModal = true;
  }

  /**
   * Handles the form submission to send the rapprochement request.
   */
  submitRapprochement(): void {
    if (this.rapprochementForm.invalid || !this.selectedDroitConstate) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires.',
      });
      return;
    }

    const dcTransactionRequest: DroitConstateTransactionRequest = {
      droitConstateId: this.selectedDroitConstate.id,
      banqueId: this.selectedDroitConstate.banqueId,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateValeur: this.rapprochementForm.value.dateValeur,
      tva: this.rapprochementForm.value.tva, // Included TVA value
    };

    this.rapprochementService.droitConstateToRapprochement(dcTransactionRequest)
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Droit constaté validé et rapproché avec succès.',
          });
          this.dt?.reset();
          this.displayModal = false;
        },
        error: (error) => {
          console.error('Erreur lors du rapprochement du droit constaté:', error);
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
      this.loadEncoursRemboursements();
    }
  }

  /**
   * Loads outstanding dc repayment records with search criteria.
   */
  loadEncoursRemboursements(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursDroitConstateSearchDto = this.searchForm.value;

    this.droitConstateService
      .getEncoursRemboursement(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (response: EncoursDroitConstateDto) => {
          this.droitConstates = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
        },
        error: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Failed to load outstanding dc records.',
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
