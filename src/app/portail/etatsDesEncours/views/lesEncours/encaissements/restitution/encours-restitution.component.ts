import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
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
import {debounceTime, distinctUntilChanged, reduce, Subscription} from "rxjs";

import { ValidationService } from "../../../../services/validation/validation.service";
import { RestitutionService } from "../../../../../restitution/service/restitution.service";
import { RestitutionResponseDto } from "../../../../../restitution/dtos/RestitutionResponseDto";
import { RestitutionSearchDto } from "../../../../../restitution/dtos/RestitutionSearchDto";
import { EncoursRestitution } from "../../../../../restitution/dtos/EncoursRestitution";
import { RestitutionValidationRequest } from "../../../../dtos/restitution/RestitutionValidationRequest";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-encours-restitution',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ToastModule, TooltipModule, DatePipe, DecimalPipe, NgFor,
    InputTextModule, CardModule, DialogModule, ReactiveFormsModule, CalendarModule,
    InputTextareaModule, ConfirmDialogModule
  ],
  templateUrl: './encours-restitution.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class EncoursRestitutionComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  restitutions: RestitutionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId: number | undefined ;
  displayModal: boolean = false;
  selectedRestitution: any = null;
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

  constructor(
    private restitutionService: RestitutionService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe: DecimalPipe,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: ValidationService,
    private bankStateService: BankStateService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/restitutions', label: 'Restitutions' },
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
          this.loadRestitutions({});
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

    this.setupSearchSubscription();

    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required],
      tvaCollectee: [null, Validators.required],
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

  loadRestitutions(event: TableLazyLoadEvent) {
    this.loading = true;

    if (!this.banqueId) {
      this.restitutions = [];
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

    const searchCriteria: RestitutionSearchDto = this.searchForm.getRawValue();

    this.restitutionService.getOutstandingRestitutions(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursRestitution) => {
          this.restitutions = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les restitutions en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  openRapprochementModal(restitution: any): void {
    this.selectedRestitution = restitution;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(restitution.dateEmission)!,
      observation: restitution.observation,
      libelle: restitution.libelle,
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

    if (this.rapprochementForm.valid && this.selectedRestitution) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedRestitution = this.selectedRestitution;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
        Êtes-vous sûr de vouloir rapprocher cette Restitution ?
        <br><br>
        <strong>ID Restitution:</strong> ${selectedRestitution.id}
        <br>
        <strong>Montant:</strong> ${this.decimalPipe.transform(selectedRestitution.montant, '1.2-2', 'fr-FR')} MAD
        <br><br>
        <strong>Détails du Rapprochement:</strong>
        <ul>
          <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
          <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
          <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
          <li><strong>Libellé:</strong> ${formValue.libelle}</li>
          <li><strong>Observation:</strong> ${formValue.observation}</li>
          <li><strong>TVA Collectée:</strong> ${formValue.tvaCollectee}%</li>
        </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Restitution',
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
    if (this.rapprochementForm.valid && this.selectedRestitution) {

      const request: RestitutionValidationRequest = {
        idRestitution: this.selectedRestitution.id,
        dateOperation: this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission: this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaCollectee
      };

      this.isSubmitting = true;

      this.rapprochementService.restitutionToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Restitution validée avec succès.',
          });
          this.displayModal = false;
          this.isSubmitting = false;
          this.table.reset();
          this.bankStateService.refreshBankInfo(this.banqueId as number);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Une erreur est survenue lors de la validation de la restitution.',
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
    this.searchSubscription.unsubscribe();
  }

  protected readonly reduce = reduce;
}
