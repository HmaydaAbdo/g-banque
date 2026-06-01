import {Component, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {CommonModule, DatePipe, DecimalPipe, NgFor} from "@angular/common";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {DropdownModule} from "primeng/dropdown";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {InputTextModule} from "primeng/inputtext";
import {CardModule} from "primeng/card";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {ConfirmationService, MenuItem, MessageService, SelectItem} from "primeng/api";
import {ActivatedRoute} from "@angular/router";
import {debounceTime, distinctUntilChanged, Subscription} from "rxjs";

import {ValidationService} from "../../../../services/validation/validation.service";
import {ImpayeeResponseDto} from "../../../../../impayees/dtos/ImpayeeResponseDto";
import {ImpayeeSearchDto} from "../../../../../impayees/dtos/ImpayeeSearchDto";
import {EncoursImpayeeDto} from "../../../../../impayees/dtos/EncoursImpayeeDto";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import {ImpayeeService} from "../../../../../impayees/service/ImpayeeService";
import {ImpayeeValidationRequest} from "../../../../dtos/impayee/Impayeevalidationrequest";
import {Type} from "../../../../../Virements/Type";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-decaissement-encours-impaye',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ToastModule, TooltipModule, DatePipe, DecimalPipe, NgFor,
    InputTextModule, CardModule, DialogModule, ReactiveFormsModule, CalendarModule,
    InputTextareaModule, ConfirmDialogModule
  ],
  templateUrl: './decaissement-encours-impayee.html',
  providers: [MessageService, ConfirmationService, DecimalPipe]
})
export class DecaissementEncoursImpayeeComponent implements OnInit, OnDestroy {
  @ViewChild('dt') table!: Table;

  impayees: ImpayeeResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId: number | undefined;
  displayModal: boolean = false;
  selectedImpayee: any = null;
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
    private impayeeService: ImpayeeService,
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
      { icon: 'pi pi-list', route: '/portail/impayees', label: 'Impayées' },
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
          this.loadImpayees({});
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

  loadImpayees(event: TableLazyLoadEvent) {
    this.loading = true;

    if (!this.banqueId) {
      this.impayees = [];
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

    const searchCriteria: ImpayeeSearchDto = this.searchForm.getRawValue();
    searchCriteria.type=Type.DECAISSEMENT

    this.impayeeService.getOutstandingImpayees(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursImpayeeDto) => {
          this.impayees = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les impayées en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  openRapprochementModal(impayee: any): void {
    this.selectedImpayee = impayee;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(impayee.dateEmission)!,
      observation: impayee.observation,
      libelle: impayee.libelle,
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

    if (this.rapprochementForm.valid && this.selectedImpayee) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedImpayee = this.selectedImpayee;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
        Êtes-vous sûr de vouloir rapprocher cette Impayée ?
        <br><br>
        <strong>ID Impayée:</strong> ${selectedImpayee.id}
        <br>
        <strong>Montant:</strong> ${this.decimalPipe.transform(selectedImpayee.montant, '1.2-2', 'fr-FR')} MAD
        <br>
        <strong>Banque :</strong> ${selectedImpayee.banqueNom}
        <br>
        <strong>Motif:</strong> ${selectedImpayee.motif}
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
        header: 'Confirmation de Rapprochement Impayée',
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
    if (this.rapprochementForm.valid && this.selectedImpayee) {

      const rapprochementId = this.bankStateService.getRapprochementId();
      const request: ImpayeeValidationRequest = {
        idImpayee: this.selectedImpayee.id,
        dateOperation:this.datePipe.transform( this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission:this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur:this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tva: this.rapprochementForm.value.tvaCollectee,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;

      this.rapprochementService.impayeeToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Impayée validée avec succès.',
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
            detail: 'Une erreur est survenue lors de la validation de l\'impayée.',
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
}
