import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, MenuItem, SelectItem, ConfirmationService } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgFor } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ActivatedRoute } from '@angular/router';
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
import { AuthService } from "../../../../../../auth/services/auth.service";
import {Motif, MotifLabels} from "../../../../../impotEtTaxes/dtos/Motif";
import {ImpotEtTaxeService} from "../../../../../impotEtTaxes/service/ImpotEtTaxeService";
import {ValidationService} from "../../../../services/validation/validation.service";
import {ImpotEtTaxeValidationRequest} from "../../../../dtos/impotsEtTaxes/ImpotEtTaxeValidationRequest";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-encours-impots-taxes',
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
    ConfirmDialogModule
  ],
  templateUrl: './encours-impots-taxes.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class EncoursImpotsEtTaxesComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  impotEtTaxes: ImpotEtTaxeResponseDto[] = [];
  totalRecords = 0;
  loading = true;
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

  constructor(
    private impotEtTaxeService: ImpotEtTaxeService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private route: ActivatedRoute,
    private authService: AuthService,
    private fb: FormBuilder,
    private rapprochementService: ValidationService,
    private bankStateService: BankStateService
  ) {}

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
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les impôts et taxes en cours.'
          });
          this.loading = false;
          console.error(err);
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
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
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
      const rapprochementId = this.route.snapshot.queryParamMap.get('rapprochementId');
      const request: ImpotEtTaxeValidationRequest = {
        idImpotEtTaxe: this.selectedImpotEtTaxe.id,
        dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaDeductible,
        rapprochementId: +rapprochementId!
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
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Une erreur est survenue.',
          });
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

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }
}
