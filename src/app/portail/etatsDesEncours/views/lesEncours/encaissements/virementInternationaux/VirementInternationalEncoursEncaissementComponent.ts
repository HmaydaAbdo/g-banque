import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {ConfirmationService, MessageService} from 'primeng/api';
import {RippleModule} from 'primeng/ripple';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ValidationService} from "../../../../services/validation/validation.service";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {DividerModule} from "primeng/divider";
import {InputTextareaModule} from "primeng/inputtextarea";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import {VirementInternationalTransactionRequestDto} from "../../../../dtos/virementsInternationaux/VirementInternationalTransactionRequestDto";
import {
  VirementInternationalResponseDto
} from "../../../../../virementInternational/dtos/VirementInternationalResponseDto";
import {
  EncoursVirementsInternationalSearchDto
} from "../../../../../virementInternational/dtos/EncoursVirementsInternationalSearchDto";
import {
  VirementInternationalService
} from "../../../../../virementInternational/service/virement-international.service";
import {
  EncoursVirementInternationalResponseDto
} from "../../../../../virementInternational/dtos/EncoursVirementInternationalResponseDto";
import { parseLocalDate } from '../../../../../global/types/date.utils';

@Component({
  selector: 'app-virement-international-encours-encaissement',
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
    DividerModule,
    InputTextareaModule,
    CalendarModule,
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ConfirmDialogModule,
    RouterLink,
  ],
  templateUrl: './virement-international-encours-encaissement.component.html',
  providers: [MessageService, ConfirmationService, DecimalPipe],
})
export class VirementInternationalEncoursEncaissementComponent implements OnInit {
  virements: VirementInternationalResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours!: number;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Propriétés pour la modale de validation
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedVirement: VirementInternationalResponseDto | null = null;

  // Propriétés pour la recherche et TVA
  searchForm!: FormGroup;
  currentSearchDto: EncoursVirementsInternationalSearchDto = {};
  tvaOptions: { label: string, value: number }[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  constructor(
    private virementInternationalService: VirementInternationalService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
    private rapprochementService: ValidationService,
    private confirmationService: ConfirmationService,
    private decimalPipe: DecimalPipe,
    private fb: FormBuilder,
    private bankStateService: BankStateService,
  ) {}

  ngOnInit(): void {
    // Récupérer l'ID de la banque depuis les paramètres de la route parente
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingVirements();
      }
    });
    this.initSearchForm();
    this.initRapprochementForm();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      dateEmission: [''],
      emetteur: [''],
      montant: [null]
    });

    this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe((searchDto: EncoursVirementsInternationalSearchDto) => {
        this.currentSearchDto = searchDto;
        this.loadOutstandingVirements();
      });
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tva: [0, Validators.required],
      libelle: ['', Validators.required],
    });
  }

  openRapprochementModal(virement: VirementInternationalResponseDto): void {
    this.selectedVirement = virement;
    this.initRapprochementForm();

    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(virement.dateEmission)!,
      libelle: virement.libelle,
      observation: virement.observation
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

    if (this.rapprochementForm.valid && this.selectedVirement) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedVirement = this.selectedVirement;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
        Êtes-vous sûr de vouloir rapprocher ce virement international ?
        <br><br>
        <strong>Émetteur:</strong> ${selectedVirement.emetteur || 'N/A'}
        <br>
        <strong>Bénéficiaire:</strong> ${selectedVirement.beneficiaire}
        <br>
        <strong>Montant:</strong> ${this.decimalPipe.transform(selectedVirement.montant, "1.2-2", 'fr-FR')} MAD
        <br><br>
        <strong>Détails du Rapprochement:</strong>
        <ul>
          <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
          <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
          <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
          <li><strong>Libellé:</strong> ${formValue.libelle}</li>
          <li><strong>Observation:</strong> ${formValue.observation}</li>
          <li><strong>TVA:</strong> ${formValue.tva}%</li>
        </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Virement International',
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

  submitRapprochement(): void {
    if (this.rapprochementForm.valid && this.selectedVirement) {
      const formValue = this.rapprochementForm.getRawValue();
      const rapprochementId = this.bankStateService.getRapprochementId();

      const virementInternationalTransactionRequest: VirementInternationalTransactionRequestDto = {
        id: this.selectedVirement.id,
        dateEmission: this.datePipe.transform(formValue.dateEmission,'yyyy-MM-dd') as string ,
        dateValeur: this.datePipe.transform(formValue.dateValeur,'yyyy-MM-dd' ) as string,
        dateOperation:this.datePipe.transform(formValue.dateOperation,'yyyy-MM-dd') as string,
        observation: formValue.observation,
        tva: formValue.tva,
        transactionSource: TransactionSource.VIREMENT_INTERNATIONAL,
        libelle: formValue.libelle,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;
      this.rapprochementService.virementInternationalEncaissementToRapprochement(virementInternationalTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Virement international rapproché avec succès.'
            });
            this.isSubmitting = false;
            this.loadOutstandingVirements();
            this.displayModal = false;
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
            this.bankStateService.notifyValidation();
          },
          error: (error) => {
            console.error('Erreur lors de la validation du virement international:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message || 'Une erreur est survenue lors de la validation.'
            });
            this.isSubmitting = false;
          }
        });
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  /**
   * Charge les virements internationaux en cours pour la banque.
   * @param event L'événement de chargement lazy de la table PrimeNG.
   */
  loadOutstandingVirements(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.virementInternationalService
      .getEncoursEncaissement(this.banqueIdFromRoute, this.currentSearchDto, page, size, sort)
      .subscribe({
        next: (response: EncoursVirementInternationalResponseDto) => {
          this.virements = response.page.content;
          this.totalRecords = response.page.page.totalElements;
          this.loading = false;
          this.totalEncours = response.totalDesEncours;
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des virements internationaux en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des virements internationaux en cours.',
          });
        },
      });
  }


}
