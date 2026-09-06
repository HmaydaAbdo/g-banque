import { Component, inject, OnInit, ViewChild } from "@angular/core";
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
import { TagModule } from "primeng/tag";
import { RouterLink } from "@angular/router";

import { ValidationService } from "../../../../services/validation/validation.service";
import { BankStateService } from "../../../../../comptes-bancaire/services/bank-state.service";
import { ProvisionService } from "../../../../../provision/service/provision.service";
import { ProvisionSearchDto } from "../../../../../provision/dtos/ProvisionSearchDto";
import { EncoursProvisionDto } from "../../../../../provision/dtos/EncoursProvisionDto";
import { ProvisionResponseDto } from "../../../../../provision/dtos/ProvisionResponseDto";
import { ProvisionValidationRequest } from "../../../../dtos/provision/ProvisionValidationRequest";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { SensOperation } from "../../../../../transactions/dtos/SensOperation";
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";
import { TableSelectionState } from "../../shared/table-selection.state";

/**
 * Encours des provisions de sens ENCAISSEMENT.
 * Provision porte les deux sens sur la même entité (comme Virement/Chèque/LCN/Mise à
 * disposition/Virement International) : un seul endpoint backend GET /provision/encours/{banqueId}
 * filtré par sens, et — suivant le même pattern que ces modules — deux écrans frontend distincts
 * (voir le pendant DECAISSEMENT dans etatsDesEncours/views/lesEncours/decaissement/provision/).
 * Contrairement à Virement (deux endpoints de validation distincts), Provision n'a qu'un seul
 * endpoint de validation, POST /rapprochement/process/provision : le sens de la Transaction
 * générée est déterminé côté backend par provision.getSens(), pas par l'écran appelant.
 */
@Component({
  selector: 'app-provision-encours-encaissement',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
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
    TagModule,
    ValidationLotModalComponent
  ],
  templateUrl: './provision-encours.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class ProvisionEncoursEncaissementComponent implements OnInit {
  provisions: ProvisionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;
  totalEncours!: number;
  banqueId?: number;

  readonly sens = SensOperation.ENCAISSEMENT;
  readonly tvaLabel = 'TVA Collectée';
  readonly totalBandLabel = 'Total encaissements';
  readonly totalBandClass: 'total-band--debit' | 'total-band--credit' = 'total-band--credit';

  // Validation en lot
  selectionState = new TableSelectionState<ProvisionResponseDto>(p => p.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): ProvisionResponseDto[] {
    return this.selectionState.getPageSelection(this.provisions);
  }

  onSelectionChange(selection: ProvisionResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.provisions, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(p => p.montant);
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

  displayModal: boolean = false;
  selectedProvision: ProvisionResponseDto | null = null;
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

  private readonly provisionService = inject(ProvisionService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly rapprochementService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);

  @ViewChild('dt') table!: Table;

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-wallet', route: '/portail/provisions', label: 'Provisions' },
      { label: 'Encours (encaissements)' }
    ];

    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;
        if (this.table) {
          this.table.reset();
        } else {
          this.loadProvisions({});
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

  clearSearch(): void {
    this.searchForm.reset({ libelle: '', montant: null });
    if (this.table) {
      this.table.clear();
      this.table.first = 0;
      this.loadProvisions(this.table.createLazyLoadMetadata());
    }
  }

  loadProvisions(event: TableLazyLoadEvent) {
    this.loading = true;

    if (!this.banqueId) {
      this.provisions = [];
      this.totalRecords = 0;
      this.totalEncours = 0;
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: ProvisionSearchDto = { ...this.searchForm.getRawValue(), sens: this.sens };

    this.provisionService.getOutstandingProvision(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursProvisionDto) => {
          this.provisions = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  openRapprochementModal(provision: ProvisionResponseDto): void {
    this.selectedProvision = provision;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(provision.dateEmission)!,
      observation: provision.observation,
      libelle: provision.libelle,
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

    if (this.rapprochementForm.valid && this.selectedProvision) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedProvision = this.selectedProvision;

      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      const recapMessage = `
        Êtes-vous sûr de vouloir rapprocher cette provision (encaissement) ?
        <br><br>
        <strong>ID Provision:</strong> ${selectedProvision.id}
        <br>
        <strong>Montant:</strong> ${this.decimalPipe.transform(selectedProvision.montant, '1.2-2', "fr-FR")} MAD
        <br>
        <strong>Motif:</strong> ${selectedProvision.motif}
        <br><br>
        <strong>Détails du Rapprochement:</strong>
        <ul>
            <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
            <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
            <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
            <li><strong>Libellé:</strong> ${formValue.libelle}</li>
            <li><strong>Observation:</strong> ${formValue.observation}</li>
            <li><strong>${this.tvaLabel}:</strong> ${formValue.tvaDeductible}%</li>
        </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement - Provision',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {
        }
      });
    }
  }

  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedProvision) {
      const formValue = this.rapprochementForm.value;

      const rapprochementId = this.bankStateService.getRapprochementId();
      const request: ProvisionValidationRequest = {
        idProvision: this.selectedProvision.id,
        dateOperation: this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(formValue.dateEmission, 'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(formValue.dateValeur, 'yyyy-MM-dd') as string,
        observation: formValue.observation,
        libelle: formValue.libelle,
        tva: formValue.tvaDeductible,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;

      this.rapprochementService.provisionToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Provision validée avec succès.',
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

  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  /**
   * Exporte en PDF l'intégralité de l'encours de provisions (décaissements) de la banque courante
   * correspondant aux filtres actuellement appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    if (!this.banqueId) {
      return;
    }
    this.exporting = true;
    const searchCriteria: ProvisionSearchDto = { ...this.searchForm.getRawValue(), sens: this.sens };

    this.provisionService.exportEncoursPdf(this.banqueId, searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `provisions_encours_encaissements_${new Date().toISOString().slice(0, 10)}.pdf`;
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

  ngOnDestroy(): void {
    if (this.searchFormSubscription) {
      this.searchFormSubscription.unsubscribe();
    }
  }
}
