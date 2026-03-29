import { Component, OnInit, ViewChild } from "@angular/core";
import { CommonModule, DatePipe, DecimalPipe, NgFor } from "@angular/common";
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { Table, TableLazyLoadEvent, TableModule } from "primeng/table";
import { DropdownModule } from "primeng/dropdown";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ToastModule } from "primeng/toast";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { CardModule } from "primeng/card";
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";
import { InputTextareaModule } from "primeng/inputtextarea";
import { ConfirmationService, MenuItem, MessageService, SelectItem } from "primeng/api";
import { ActivatedRoute } from "@angular/router";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { debounceTime, distinctUntilChanged, Subscription } from "rxjs";

import { ValidationService } from "../../../../services/validation/validation.service";
import { BankStateService } from "../../../../../comptes-bancaire/services/bank-state.service";
import { OperationErroneService } from "../../../../../operationErronées/service/operation-errone.service";
import { OperationErroneSearchDto } from "../../../../../operationErronées/dtos/OperationErroneSearchDto";
import { EncoursOperationsErroneDto } from "../../../../../operationErronées/dtos/EncoursOperationsErroneDto";
import { OperationErroneResponseDto } from "../../../../../operationErronées/dtos/OperationErroneResponseDto";
import {OperationErroneValidationRequest} from "../../../../dtos/operation-erronés/OperationErroneValidationRequest";
import {TagModule} from "primeng/tag";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-encours-operation-errone',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    BreadcrumbModule,
    ToastModule,
    TooltipModule,
    DatePipe,
    DecimalPipe,
    NgFor,
    InputTextModule,
    CardModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
    ConfirmDialogModule,
    TagModule
  ],
  templateUrl: './operation-errone.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class EncoursOperationErroneComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  operations: OperationErroneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedOperation: OperationErroneResponseDto | null = null;
  rapprochementForm!: FormGroup;

  searchForm!: FormGroup;

  items: MenuItem[] = [];
  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  searchFormSubscription!: Subscription;

  constructor(
    private operationErroneService: OperationErroneService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: ValidationService,
    private bankStateService: BankStateService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-exclamation-triangle', route: '/portail/operations/errones', label: 'Opérations Erronées' },
      { label: 'Encours' }
    ];

    // Subscribe to the parent route to get the banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        // Trigger the table's lazy load once the ID is ready
        if (this.table) {
          this.table.reset();
        } else {
          this.loadOperations({});
        }
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Aucun ID de banque trouvé dans l\'URL.'
        });
      }
    });

    this.searchForm = this.fb.group({
      libelle: [''],
      montant: [null],
    });

    this.searchFormSubscription = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.applyFilters();
    });

    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required],
      tvaDeductible: [null, Validators.required],
    });
  }

  loadOperations(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.operations = [];
      this.totalRecords = 0;
      this.totalEncours = 0;
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: OperationErroneSearchDto = this.searchForm.getRawValue();

    this.operationErroneService.getOutstandingOperationErrone(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursOperationsErroneDto) => {
          this.operations = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les opérations erronées en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(operation: OperationErroneResponseDto): void {
    this.selectedOperation = operation;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(operation.dateEmission)!,
      observation: operation.observation,
      libelle: operation.libelle,
    });
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

    if (this.rapprochementForm.valid && this.selectedOperation) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedOperation = this.selectedOperation;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
        Êtes-vous sûr de vouloir rapprocher cette opération erronée ?
        <br><br>
        <strong>ID Opération:</strong> ${selectedOperation.id}
        <br>
        <strong>Montant:</strong> ${this.decimalPipe.transform(selectedOperation.montant, '1.2-2', "fr-FR")} MAD
        <br>
        <strong>Motif:</strong> ${selectedOperation.motif}
        <br><br>
        <strong>Détails du Rapprochement:</strong>
        <ul>
            <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
            <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
            <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
            <li><strong>Libellé:</strong> ${formValue.libelle}</li>
            <li><strong>Observation:</strong> ${formValue.observation}</li>
            <li><strong>TVA Déductible:</strong> ${formValue.tvaDeductible}%</li>
        </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement - Opération Erronée',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {
          // L'utilisateur a annulé, aucune action
        }
      });
    }
  }

  // Soumettre la validation
  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedOperation) {
      const formValue = this.rapprochementForm.value;

      const request: OperationErroneValidationRequest = {
        idOperationErrone: this.selectedOperation.id,
        dateOperation: this.datePipe.transform(formValue.dateOperation,'yyyy-MM-dd') as string,
        dateEmission:this.datePipe.transform(formValue.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(formValue.dateValeur,'yyyy-MM-dd') as string,
        observation: formValue.observation,
        libelle: formValue.libelle,
        tva: formValue.tvaDeductible
      };

      this.isSubmitting = true;

      this.rapprochementService.operationErroneToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Opération erronée validée avec succès.',
          });
          this.isSubmitting = false;
          this.displayModal = false;
          this.table.reset();
          this.bankStateService.refreshBankInfo(this.banqueId as number);
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: err.error?.message || 'Une erreur est survenue lors de la validation.',
          });
          this.isSubmitting = false;
        },
      });
    }
  }

  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  ngOnDestroy(): void {
    if (this.searchFormSubscription) {
      this.searchFormSubscription.unsubscribe();
    }
  }
}
