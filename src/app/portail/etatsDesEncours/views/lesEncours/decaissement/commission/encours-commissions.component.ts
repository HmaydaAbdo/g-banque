import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import {MessageService, MenuItem, SelectItem, ConfirmationService} from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgFor } from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ActivatedRoute } from '@angular/router';
import { InputTextModule } from "primeng/inputtext";
import { CardModule } from 'primeng/card';
import { Decimal } from 'decimal.js';
import {CommissionResponseDto} from "../../../../../commission/dtos/commission-response.dto";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {CommissionService} from "../../../../../commission/service/commission.service";
import {CommissionSearchDto} from "../../../../../commission/dtos/CommissionSearchDto";
import {EncoursCommissionDto} from "../../../../../commission/dtos/EncoursCommissionDto";
import {CommissionValidationDto} from "../../../../dtos/commission/CommissionValidationDto";
import {ValidationService} from "../../../../services/validation/validation.service";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {debounceTime, distinctUntilChanged, Subscription} from "rxjs";
import {AuthService} from "../../../../../../auth/services/auth.service";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";

@Component({
  selector: 'app-encours-commissions',
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
  templateUrl: './encours-commissions.component.html',
  providers: [MessageService,ConfirmationService,DecimalPipe]
})
export class EncoursCommissionsComponent implements OnInit ,OnDestroy{
  @ViewChild('dt') table!: Table;

  commissions: CommissionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedCommission!: CommissionResponseDto ;
  rapprochementForm!: FormGroup;

  searchForm!: FormGroup;
  private searchSubscription!: Subscription;


  operationOptions: { label: string; value: TransactionSource | null }[] = [];
  items: MenuItem[] = [];
  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  constructor(
    private commissionService: CommissionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private route: ActivatedRoute,
    private authService:AuthService,
    private fb: FormBuilder,
    private rapprochementService: ValidationService,
    private bankStateService: BankStateService

  ) {}


  ngOnInit(): void {

    this.searchForm = this.fb.group({
      operation: [null], // Filtre par type d'opération
      libelle: [''], // Filtre par libellé
      montant: [null] // Nouveau filtre par montant
    });
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/commissions', label: 'Commissions' },
      { label: 'Encours' }
    ];

    this.setupSearchSubscription();
    this.loadOperationOptions();

    // Subscribe to the parent route to get the banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        // Trigger the table's lazy load once the ID is ready
        if (this.table) {
          this.table.reset();
        } else {
          this.loadCommissions({});
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
    // Écoute les changements de valeur sur l'ensemble du formulaire
    this.searchSubscription = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
    ).subscribe(() => {
      this.applyFilters();
    });
  }

  loadCommissions(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.commissions = [];
      this.totalRecords = 0;
      this.totalEncours =0;
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const formValues = this.searchForm.value; // Récupérer toutes les valeurs

    const searchCriteria: CommissionSearchDto = {
      operation: formValues.operation,
      libelle: formValues.libelle ,
      montant: formValues.montant
    };

    this.commissionService.getOutstandingCommissions(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursCommissionDto) => {
          this.commissions = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les commissions en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(commission: any): void {
    this.selectedCommission = commission;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: new Date(commission.dateEmission),
      observation: commission.observation,
      libelle: commission.libelle,
    })
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

    if (this.rapprochementForm.valid && this.selectedCommission) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedCommission = this.selectedCommission;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher cette Commission ?
            <br><br>
            <strong>ID Commission:</strong> ${selectedCommission.id}
            <br>
            <strong>Type d'Opération:</strong> ${selectedCommission.operation}
            <br>
            <strong>Montant TTC:</strong> ${this.decimalPipe.transform(selectedCommission.montant,'1.2-2','fr-FR')} MAD
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
        header: 'Confirmation de Rapprochement Commission',
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
    if (this.rapprochementForm.valid && this.selectedCommission) {

      const request: CommissionValidationDto = {
        idCommission: this.selectedCommission.id,
        dateOperation:this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaDeductible
      };

      this.isSubmitting = true;
      this.rapprochementService.commissionToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Commission validée avec succès.',
          });
          this.isSubmitting = false;
          this.displayModal = false;
          this.table.reset(); // recharge ton tableau existant
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

  loadOperationOptions() {
    this.operationOptions = Object.values(TransactionSource).map(op => ({
      label: op,
      value: op
    }));
  }

  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters() {
    this.searchForm.reset({
      operation: null,
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
