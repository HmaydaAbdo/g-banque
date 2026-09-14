import { Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { CalendarModule } from 'primeng/calendar';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ConfirmationService, MenuItem, MessageService, SelectItem } from 'primeng/api';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { debounceTime, distinctUntilChanged, Subscription } from 'rxjs';

import { AgiosService } from '../../../../../agios/service/agios.service';
import { AgiosResponseDto } from '../../../../../agios/dtos/AgiosResponseDto';
import { AgiosSearchDto } from '../../../../../agios/dtos/AgiosSearchDto';
import { EncoursAgiosDto } from '../../../../../agios/dtos/EncoursAgiosDto';
import { AgiosValidationRequest } from '../../../../dtos/agios/agios-validation-request.dto';
import { ValidationService } from '../../../../services/validation/validation.service';
import { BankStateService } from '../../../../../comptes-bancaire/services/bank-state.service';
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from '../../../../../transactions/dtos/TransactionSource';
import { BatchValidationResponseDto } from '../../../../dtos/lot/BatchValidationResponseDto';
import { ValidationLotModalComponent } from '../../shared/validation-lot-modal/validation-lot-modal.component';

import {TvaInputComponent} from "../../shared/tva-input/tva-input.component";
import {
  decrireSaisieTva,
  messageErreurTva,
  SaisieTva,
  saisieTvaParDefaut
} from "../../shared/tva-input/saisie-tva.model";import { TableSelectionState } from '../../shared/table-selection.state';

@Component({
  selector: 'app-encours-agios',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    TableModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    BreadcrumbModule,
    ToastModule,
    TooltipModule,
    InputTextModule,
    CardModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
    ConfirmDialogModule,
    RouterLink,
    ValidationLotModalComponent,
    TvaInputComponent
  ],
  templateUrl: './encours-agios.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe, DatePipe]
})
export class EncoursAgiosComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  private readonly agiosService        = inject(AgiosService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe            = inject(DatePipe);
  private readonly route               = inject(ActivatedRoute);
  private readonly fb                  = inject(FormBuilder);
  private readonly rapprochementService = inject(ValidationService);
  private readonly bankStateService    = inject(BankStateService);

  agios: AgiosResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;
  totalEncours = 0;
  banqueId?: number;

  displayModal = false;
  selectedAgio: AgiosResponseDto | null = null;

  searchForm = this.fb.group({
    libelle: [''],
    montant: [null as number | null],
  });

  rapprochementForm = this.fb.group({
    dateEmission:  [null as Date | null, Validators.required],
    dateOperation: [null as Date | null, Validators.required],
    dateValeur:    [null as Date | null, Validators.required],
    observation:   ['', Validators.required],
    libelle:       ['', Validators.required],
    tva: [saisieTvaParDefaut()],
  });

  items: MenuItem[] = [];

  isSubmitting = false;

  /** Exposé au template pour le message d'erreur du champ TVA. */
  readonly messageErreurTva = messageErreurTva;

  /** Borne haute du montant de TVA saisissable, exposée au template. */
  get montantMaxTva(): number | null {
    return this.selectedAgio?.montant ?? null;
  }
  private searchSubscription!: Subscription;

  // Validation en lot
  selectionState = new TableSelectionState<AgiosResponseDto>(a => a.id);
  displayLotModal = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): AgiosResponseDto[] {
    return this.selectionState.getPageSelection(this.agios);
  }

  onSelectionChange(selection: AgiosResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.agios, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(a => a.montant);
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

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/agios',   label: 'Agios'   },
      { label: 'Encours' }
    ];

    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;
        if (this.table) {
          this.table.reset();
        } else {
          this.loadAgios({});
        }
      } else {
        this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Aucun ID de banque trouvé dans l\'URL.' });
      }
    });

    this.setupSearchSubscription();
  }

  setupSearchSubscription(): void {
    this.searchSubscription = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => this.applyFilters());
  }

  clearSearch(): void {
    this.searchForm.reset({ libelle: '', montant: null });
    if (this.table) {
      this.table.clear();
      this.table.first = 0;
      this.loadAgios(this.table.createLazyLoadMetadata());
    }
  }

  loadAgios(event: TableLazyLoadEvent): void {
    this.loading = true;

    if (!this.banqueId) {
      this.agios        = [];
      this.totalRecords = 0;
      this.totalEncours = 0;
      this.loading      = false;
      return;
    }

    const page      = event.first ? event.first / (event.rows ?? 10) : 0;
    const size      = event.rows ?? 10;
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';

    const searchCriteria: AgiosSearchDto = this.searchForm.getRawValue() as AgiosSearchDto;

    this.agiosService.getOutstandingAgios(this.banqueId, searchCriteria, page, size, `${sortField},${sortOrder}`)
      .subscribe({
        next: (res: EncoursAgiosDto) => {
          this.agios        = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading      = false;
        },
        error: () => { this.loading = false; }
      });
  }

  openRapprochementModal(agios: AgiosResponseDto): void {
    this.selectedAgio = agios;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(agios.dateEmission)!,
      observation:  agios.observation,
      libelle:      agios.libelle,
    });
    this.displayModal = true;
  }

  confirmRapprochement(): void {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      return;
    }

    if (this.rapprochementForm.valid && this.selectedAgio) {
      const formValue = this.rapprochementForm.getRawValue();

      this.confirmationService.confirm({
        message: `Confirmer le rapprochement de l'agios #${this.selectedAgio.id} — ${this.selectedAgio.montant} MAD ?`,
        header: 'Confirmation de rapprochement',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => this.submitRapprochement()
      });
    }
  }

  submitRapprochement(): void {
    if (!this.rapprochementForm.valid || !this.selectedAgio) return;

    const rapprochementId = this.bankStateService.getRapprochementId();
    const formValue = this.rapprochementForm.getRawValue();

    const saisieTva: SaisieTva = formValue.tva!;
    const request: AgiosValidationRequest = {
      idAgio:          this.selectedAgio.id,
      dateOperation:   this.datePipe.transform(formValue.dateOperation, 'yyyy-MM-dd') as string,
      dateEmission:    this.datePipe.transform(formValue.dateEmission,  'yyyy-MM-dd') as string,
      dateValeur:      this.datePipe.transform(formValue.dateValeur,    'yyyy-MM-dd') as string,
      observation:     formValue.observation!,
      libelle:         formValue.libelle!,
      tauxTva: saisieTva.tauxTva,
      montantTva: saisieTva.montantTva,
      rapprochementId: rapprochementId!
    };

    this.isSubmitting = true;

    this.rapprochementService.agiosToRapprochement(request).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Agios validé avec succès.' });
        this.displayModal = false;
        this.isSubmitting = false;
        this.table.reset();
        this.bankStateService.refreshBankInfo(this.banqueId as number);
        this.bankStateService.notifyValidation();
      },
      error: () => { this.isSubmitting = false; }
    });
  }

  applyFilters(): void {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  exportPdf(): void {
    if (!this.banqueId) return;
    this.exporting = true;
    const searchCriteria: AgiosSearchDto = this.searchForm.getRawValue() as AgiosSearchDto;

    this.agiosService.exportOutstandingAgiosPdf(this.banqueId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `encours_agios_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      }
    });
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }
}
