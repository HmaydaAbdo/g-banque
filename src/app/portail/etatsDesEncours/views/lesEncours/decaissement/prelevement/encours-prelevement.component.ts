import { Component, inject, OnDestroy, OnInit, ViewChild } from "@angular/core";
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
import { debounceTime, distinctUntilChanged, Subscription } from "rxjs";

import { ValidationService } from "../../../../services/validation/validation.service";
import { PrelevementService } from "../../../../../prelevements/service/prelevement.service";
import { PrelevementResponseDto } from "../../../../../prelevements/dtos/PrelevementResponseDto";
import { PrelevementSearchDto } from "../../../../../prelevements/dtos/PrelevementSearchDto";
import { EncoursPrelevement } from "../../../../../prelevements/dtos/EncoursPrelevement";
import { PrelevementValidationRequestDto } from "../../../../dtos/prelevement/prelevement-validation-request.dto";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";


@Component({
  selector: 'app-encours-prelevement',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ToastModule, TooltipModule, DatePipe, DecimalPipe, NgFor,
    InputTextModule, CardModule, DialogModule, ReactiveFormsModule, CalendarModule,
    InputTextareaModule, ConfirmDialogModule, ValidationLotModalComponent
  ],
  templateUrl: './prelevement.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class EncoursPrelevementComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  prelevements: PrelevementResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedPrelevement: any = null;
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
  private searchSubscription!: Subscription;

  // Validation en lot
  selectionState = new TableSelectionState<PrelevementResponseDto>(p => p.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): PrelevementResponseDto[] {
    return this.selectionState.getPageSelection(this.prelevements);
  }

  onSelectionChange(selection: PrelevementResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.prelevements, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(p => p.montant as unknown as number);
  }

  clearSelection(): void {
    this.selectionState.clear();
  }

  onLotValidated(response: BatchValidationResponseDto): void {
    this.selectionState.clear();
    this.table.reset();
    this.bankStateService.refreshBankInfo(this.banqueId as number);
    this.bankStateService.notifyValidation();
  }

  private readonly prelevementService = inject(PrelevementService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly rapprochementService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);



  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/prelevements', label: 'Prélèvements' },
      { label: 'Encours' }
    ];

    // Subscription to parent route to get the banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        if (this.table) {
          this.table.reset();
        } else {
          this.loadPrelevements({});
        }
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'No bank ID found in the URL.'
        });
      }
    });


    this.searchForm = this.fb.group({
      libelle: [''],
      montant: [null],
    });

    this.setupSearchSubscription();

    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required],
      tvaDeductible: [null, Validators.required],
    });
  }

  setupSearchSubscription(): void {
    this.searchSubscription = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.applyFilters();
    });
  }

  loadPrelevements(event: TableLazyLoadEvent) {
    this.loading = true;

    if (!this.banqueId) {
      this.prelevements = [];
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

    const searchCriteria: PrelevementSearchDto = this.searchForm.getRawValue();

    this.prelevementService.getOutstandingPrelevement(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursPrelevement) => {
          this.prelevements = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
        }
      });
  }


  openRapprochementModal(prelevement: any): void {
    this.selectedPrelevement = prelevement;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(prelevement.dateEmission)!,
      observation: prelevement.observation,
      libelle: prelevement.libelle,
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

    if (this.rapprochementForm.valid && this.selectedPrelevement) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedPrelevement = this.selectedPrelevement;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');


      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ce Prélèvement ?
            <br><br>
            <strong>ID Prélèvement:</strong> ${selectedPrelevement.id}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedPrelevement.montant, '1.2-2', 'fr-FR')} MAD
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
        header: 'Confirmation de Rapprochement Prélèvement',
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

    if (this.rapprochementForm.valid && this.selectedPrelevement) {

      const rapprochementId = this.bankStateService.getRapprochementId();
      const request: PrelevementValidationRequestDto = {
        idPrelevement: this.selectedPrelevement.id,
        dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaDeductible,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;

      this.rapprochementService.prelevementToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Prélèvement validé avec succès.',
          });
          this.displayModal = false;
          this.isSubmitting = false;
          this.table.reset();
          this.bankStateService.refreshBankInfo(this.banqueId as  number);
          this.bankStateService.notifyValidation();

        },
        error: () => {
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
    this.searchSubscription.unsubscribe();
  }
}
