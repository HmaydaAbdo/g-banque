import { Component, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, MenuItem, SelectItem, ConfirmationService } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InputTextModule } from "primeng/inputtext";
import { CardModule } from 'primeng/card';
import { DialogModule } from "primeng/dialog";
import { CalendarModule } from "primeng/calendar";
import { InputTextareaModule } from "primeng/inputtextarea";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { debounceTime, distinctUntilChanged, Subscription } from "rxjs";
import { ImpotEtTaxeResponseDto } from "../../../../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto";
import { ImpotEtTaxeSearchDto } from "../../../../../impotEtTaxes/dtos/ImpotEtTaxeSearchDto";
import { EncoursImpotEtTaxeDto } from "../../../../../impotEtTaxes/dtos/EncoursImpotEtTaxeDto";
import {Motif, MotifLabels} from "../../../../../impotEtTaxes/dtos/Motif";
import {ImpotEtTaxeService} from "../../../../../impotEtTaxes/service/ImpotEtTaxeService";
import {ValidationService} from "../../../../services/validation/validation.service";
import {ImpotEtTaxeValidationRequest} from "../../../../dtos/impotsEtTaxes/ImpotEtTaxeValidationRequest";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";

@Component({
  selector: 'app-encours-impots-taxes',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    BreadcrumbModule,
    ToastModule,
    TooltipModule,
    DatePipe,
    DecimalPipe,
    InputTextModule,
    CardModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
    ConfirmDialogModule,
    RouterLink,
    ValidationLotModalComponent
  ],
  templateUrl: './encours-impots-taxes.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class EncoursImpotsEtTaxesComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  impotEtTaxes: ImpotEtTaxeResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedImpotEtTaxe!: ImpotEtTaxeResponseDto;
  rapprochementForm!: FormGroup;

  searchForm!: FormGroup;
  private searchSubscription!: Subscription;

  motifOptions: { label: string; value: Motif | null }[] = [];
  motifLabels = MotifLabels;
  items: MenuItem[] = [];
  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 },
    { label: '100%', value: 100 }
  ];

  isSubmitting: boolean = false;

  // Validation en lot
  selectionState = new TableSelectionState<ImpotEtTaxeResponseDto>(i => i.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): ImpotEtTaxeResponseDto[] {
    return this.selectionState.getPageSelection(this.impotEtTaxes);
  }

  onSelectionChange(selection: ImpotEtTaxeResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.impotEtTaxes, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(i => i.montant);
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

  private readonly impotEtTaxeService = inject(ImpotEtTaxeService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly rapprochementService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {

    this.searchForm = this.fb.group({
      motif: [null],
      libelle: [''],
      montant: [null]
    });

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/impots-taxes', label: 'Impôts et Taxes' },
      { label: 'Encours' }
    ];

    this.setupSearchSubscription();
    this.loadMotifOptions();

    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        if (this.table) {
          this.table.reset();
        } else {
          this.loadImpotEtTaxes({});
        }
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'No bank ID found in the URL.'
        });
      }
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

  setupSearchSubscription(): void {
    this.searchSubscription = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
    ).subscribe(() => {
      this.applyFilters();
    });
  }

  loadImpotEtTaxes(event: TableLazyLoadEvent) {
    this.loading = true;

    if (!this.banqueId) {
      this.impotEtTaxes = [];
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

    const formValues = this.searchForm.value;

    const searchCriteria: ImpotEtTaxeSearchDto = {
      motif: formValues.motif,
      libelle: formValues.libelle,
      montant: formValues.montant
    };

    this.impotEtTaxeService.getOutstandingImpotEtTaxes(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursImpotEtTaxeDto) => {
          this.impotEtTaxes = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  openRapprochementModal(impotEtTaxe: any): void {
    this.selectedImpotEtTaxe = impotEtTaxe;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(impotEtTaxe.dateEmission)!,
      observation: impotEtTaxe.observation,
      libelle: impotEtTaxe.libelle,
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

    if (this.rapprochementForm.valid && this.selectedImpotEtTaxe) {
      const formValue = this.rapprochementForm.getRawValue();
      const selected = this.selectedImpotEtTaxe;

      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');
      const libelleEchappe = this.escapeHtml(formValue.libelle);
      const observationEchappee = this.escapeHtml(formValue.observation);

      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher cet Impôt/Taxe ?
            <br><br>
            <strong>ID:</strong> ${selected.id}
            <br>
            <strong>Motif:</strong> ${this.motifLabels[selected.motif]}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selected.montant, '1.2-2', 'fr-FR')} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${libelleEchappe}</li>
                <li><strong>Observation:</strong> ${observationEchappee}</li>
                <li><strong>TVA Déductible:</strong> ${formValue.tvaDeductible}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Impôt/Taxe',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {
          // L'utilisateur a annulé
        }
      });
    }
  }

  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedImpotEtTaxe) {
      const rapprochementId = this.bankStateService.getRapprochementId();
      const request: ImpotEtTaxeValidationRequest = {
        idImpotEtTaxe: this.selectedImpotEtTaxe.id,
        dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaDeductible,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;
      this.rapprochementService.impotEtTaxeToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Impôt/Taxe validé(e) avec succès.',
          });
          this.isSubmitting = false;
          this.displayModal = false;
          this.table.reset();
          this.bankStateService.refreshBankInfo(this.banqueId as number);
          this.bankStateService.notifyValidation();
        },
        error: () => {
          this.isSubmitting = false;
        },
      });
    }
  }

  loadMotifOptions() {
    this.motifOptions = Object.values(Motif).map(motif => ({
      label: MotifLabels[motif],
      value: motif
    }));
  }

  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters() {
    this.searchForm.reset({
      motif: null,
      libelle: '',
      montant: null
    });
    this.applyFilters();
  }

  /**
   * Exporte en PDF l'intégralité de l'encours d'impôts/taxes de la banque courante, filtré selon
   * les critères actuellement appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    if (!this.banqueId) {
      return;
    }
    this.exporting = true;
    const formValues = this.searchForm.value;
    const searchCriteria: ImpotEtTaxeSearchDto = {
      motif: formValues.motif,
      libelle: formValues.libelle,
      montant: formValues.montant
    };

    this.impotEtTaxeService.exportEncoursPdf(this.banqueId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `impots_taxes_encours_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  /**
   * Échappe les caractères HTML spéciaux avant interpolation dans un message de confirmation
   * PrimeNG (rendu via [innerHTML] par p-confirmDialog) — libellé et observation sont des
   * données libres saisies par un utilisateur Finance (Frontend.md, section Sécurité).
   */
  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }
}
