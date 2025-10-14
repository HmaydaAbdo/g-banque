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
import {PrefinancementResponseDTO} from "../../../../../prefinancements/dtos/PrefinancementResponseDTO";
import {PrefinancementService} from "../../../../../prefinancements/service/PrefinancementService";
import {EncoursPrefinancementDto} from "../../../../../prefinancements/dtos/EncoursPrefinancementDto";
import {Decimal} from "decimal.js";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {
  PrefinancementTransactionRequest
} from "../../../../rapprochement/dtos/prefinancement/PrefinancementTransactionRequest";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import { EncoursPrefinancementSearchDto } from "../../../../../prefinancements/dtos/EncoursPrefinancementSearchDto";
import { InputTextModule } from 'primeng/inputtext';
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import { ActivatedRoute } from '@angular/router'; // Import ActivatedRoute

import {  Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { DropdownModule } from 'primeng/dropdown';
import {InputTextareaModule} from "primeng/inputtextarea";


@Component({
  selector: 'app-prefinancement-remboursements',
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
    InputTextareaModule,
  ],
  templateUrl: './prefinancement-remboursements.component.html',
  styleUrl: './prefinancement-remboursements.component.scss',
  providers: [MessageService],
})
export class PrefinancementRemboursementsComponent implements OnInit, OnDestroy {
  prefinancements: PrefinancementResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueId!: number; // Remove hardcoded value
  totalEncours!: Decimal;
  @ViewChild('dt') dt: Table | undefined;

  // Search form properties
  searchForm!: FormGroup;
  private searchSubscription!: Subscription;

  // properties for the dialog and form
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedPrefinancement: PrefinancementResponseDTO | null = null;

  // New TVA options for the dialog dropdown
  tvaOptions = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
  ];

  isSubmitting: boolean = false;

  constructor(
    private prefinancementService: PrefinancementService,
    private messageService: MessageService,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Subscribe to parent route parameters to get banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;
        // Call initSearchForm here to start the data loading process
        this.initSearchForm();
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Aucun ID de banque trouvé dans l\'URL.'
        });
      }
    });
    this.initRapprochementForm();
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

    // Initial data load triggered after banqueId is set
    this.loadEncoursRemboursements();

    // Setup valueChanges subscription after the form is initialized
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

  /**
   * Initializes the reactive form for the rapprochement data.
   * New: Added a tvaDeductible form control.
   */
  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      tvaDeductible: [null, Validators.required],
      observation:['',Validators.required],
      libelle:['',Validators.required],
    });
  }

  /**
   * Opens the dialog to validate a prefinancement.
   * @param prefinancement The prefinancement record to validate.
   */
  openRapprochementModal(prefinancement: PrefinancementResponseDTO): void {
    this.selectedPrefinancement = prefinancement;
    this.initRapprochementForm();
    this.rapprochementForm.patchValue({
      dateEmission:new Date(prefinancement.dateEmission),
      observation:prefinancement.observation,
      libelle:prefinancement.libelle
    })
    this.displayModal = true;
  }

  /**
   * Handles the form submission to send the rapprochement request.
   * New: Includes tvaDeductible in the request.
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
      dateEmission:this.rapprochementForm.value.dateEmission,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateValeur: this.rapprochementForm.value.dateValeur,
      tva: this.rapprochementForm.value.tvaDeductible,
      libelle:this.rapprochementForm.value.libelle,
      observation:this.rapprochementForm.value.observation,

    };

    this.isSubmitting = true;
    this.rapprochementService.prefinancementToRapprochement(prefinancementTransactionRequest)
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Préfinancement validé et rapproché avec succès.',
          });
          this.isSubmitting = false;
          this.dt?.reset();
          this.displayModal = false;
        },
        error: (error) => {
          console.error('Erreur lors du rapprochement du préfinancement:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error?.message || 'Une erreur est survenue lors du rapprochement.',
          });
          this.isSubmitting = false;
        },
      });
  }


  /**
   * Loads outstanding pre-financing repayment records.
   */
  loadEncoursRemboursements(event?: TableLazyLoadEvent): void {
    this.loading = true;

    // Guard clause: ensure banqueId is available before making the call
    if (!this.banqueId) {
      this.loading = false;
      return;
    }

    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    const searchCriteria: EncoursPrefinancementSearchDto = this.searchForm.value;

    this.prefinancementService
      .getEncoursRemboursement(this.banqueId, searchCriteria, page, size, sort)
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
            detail: 'Failed to load outstanding pre-financing records.',
          });
        },
      });
  }

  /**
   * Helper function to check if a form control is invalid.
   * New: Checks for the tvaDeductible form control's validity.
   */
  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
