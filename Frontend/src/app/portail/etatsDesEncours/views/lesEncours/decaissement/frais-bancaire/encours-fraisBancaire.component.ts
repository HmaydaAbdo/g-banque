import {Component, inject, OnDestroy, OnInit, ViewChild} from "@angular/core";
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
import {Decimal} from "decimal.js";
import {ActivatedRoute} from "@angular/router";
import {ValidationService} from "../../../../services/validation/validation.service";
import {FraisBancaireResponseDto} from "../../../../../frais-bancaire/dtos/FraisBancaireResponseDto";
import {FraisBancaireService} from "../../../../../frais-bancaire/service/FraisBancaireService";
import {FraisBancaireSearchDto} from "../../../../../frais-bancaire/dtos/FraisBancaireSearchDto";
import {EncoursFraisBancaireDto} from "../../../../../frais-bancaire/dtos/EncoursFraisBancaireDto";
import {FraisBancaireValidationRequest} from "../../../../dtos/frais-bancaire/frais-bancaire-validation-request.dto";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {debounceTime, distinctUntilChanged, Subscription} from "rxjs";
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';
import { TransactionSource } from "../../../../../transactions/dtos/TransactionSource";
import { BatchValidationResponseDto } from "../../../../dtos/lot/BatchValidationResponseDto";
import { ValidationLotModalComponent } from "../../shared/validation-lot-modal/validation-lot-modal.component";

import {TvaInputComponent} from "../../shared/tva-input/tva-input.component";
import {
  decrireSaisieTva,
  messageErreurTva,
  SaisieTva,
  saisieTvaParDefaut
} from "../../shared/tva-input/saisie-tva.model";import { TableSelectionState } from "../../shared/table-selection.state";

@Component({
  selector: 'app-encours-fraisBancaire',
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
    ValidationLotModalComponent,
    TvaInputComponent
  ],
  templateUrl: './encours-fraisBancaire.component.html',
  providers: [MessageService,ConfirmationService,DecimalPipe]
})
export class EncoursFraisBancaireComponent implements OnInit,OnDestroy {
  @ViewChild('dt') table!: Table;

  fraisBancaires: FraisBancaireResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedFraisBancaire: any = null;
  rapprochementForm!: FormGroup;

  searchForm!:FormGroup

  subscriptions:Subscription[]=[]

  items: MenuItem[] = [];

  isSubmitting: boolean = false;

  /** Exposé au template pour le message d'erreur du champ TVA. */
  readonly messageErreurTva = messageErreurTva;

  /** Borne haute du montant de TVA saisissable, exposée au template. */
  get montantMaxTva(): number | null {
    return this.selectedFraisBancaire?.montant ?? null;
  }

  // Validation en lot
  selectionState = new TableSelectionState<FraisBancaireResponseDto>(f => f.id);
  displayLotModal: boolean = false;
  readonly TransactionSource = TransactionSource;

  get pageSelection(): FraisBancaireResponseDto[] {
    return this.selectionState.getPageSelection(this.fraisBancaires);
  }

  onSelectionChange(selection: FraisBancaireResponseDto[]): void {
    this.selectionState.onPageSelectionChange(this.fraisBancaires, selection);
  }

  get selectedCount(): number {
    return this.selectionState.count;
  }

  get selectedIds(): number[] {
    return this.selectionState.selectedIds;
  }

  get montantTotalSelection(): number {
    return this.selectionState.sumBy(f => f.montant as unknown as number);
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

  private readonly fraisBancaireService = inject(FraisBancaireService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly rapprochementService = inject(ValidationService);
  private readonly bankStateService = inject(BankStateService);



  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/fraisBancaire', label: 'Frais Bancaire' },
      { label: 'Encours' }
    ];


    // Subscribe to the parent route to get the banqueId

   const sub1= this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        // Trigger the table's lazy load once the ID is ready
        if (this.table) {
          this.table.reset();
        } else {
          this.loadFraisBancaire({});
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
    })

   const sub2= this.searchForm.valueChanges.pipe(
      // Attends 300ms après la dernière frappe/changement
      debounceTime(300),
      // N'émet que si la valeur actuelle est différente de la dernière valeur émise
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      // Recharge le tableau via applyFilters (qui appelle table.reset())
      this.applyFilters();
    });

    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required],
      tva: [saisieTvaParDefaut()],
    });

    this.subscriptions.push(sub1,sub2)
  }

  clearSearch(): void {
    this.searchForm.reset({ libelle: '', montant: null });
    if (this.table) {
      this.table.clear();
      this.table.first = 0;
      this.loadFraisBancaire(this.table.createLazyLoadMetadata());
    }
  }

  loadFraisBancaire(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.fraisBancaires = [];
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

    const searchCriteria: FraisBancaireSearchDto = this.searchForm.getRawValue();

    this.fraisBancaireService.getOutstandingFraisBancaire(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursFraisBancaireDto) => {
          this.fraisBancaires = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(fraisBancaire: any): void {
    this.selectedFraisBancaire = fraisBancaire;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: parseLocalDate(fraisBancaire.dateEmission)!,
      observation: fraisBancaire.observation,
      libelle: fraisBancaire.libelle,
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

    if (this.rapprochementForm.valid && this.selectedFraisBancaire) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedFraisBancaire = this.selectedFraisBancaire;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');



      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ces Frais Bancaires ?
            <br><br>
            <strong>ID Frais:</strong> ${selectedFraisBancaire.id}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedFraisBancaire.montant,'1.2-2','fr-FR')} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Déductible:</strong> ${decrireSaisieTva(formValue.tva)}</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Frais Bancaires',
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
    if (this.rapprochementForm.valid && this.selectedFraisBancaire) {


      const rapprochementId = this.bankStateService.getRapprochementId();
      const saisieTva: SaisieTva = this.rapprochementForm.value.tva;
      const request: FraisBancaireValidationRequest = {
        idFraisBancaire: this.selectedFraisBancaire.id,
        dateOperation:this.datePipe.transform(this.rapprochementForm.value.dateOperation,'yyyy-MM-dd') as string,
        dateEmission:this.datePipe.transform(this.rapprochementForm.value.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform(this.rapprochementForm.value.dateValeur,'yyyy-MM-dd') as string,
        observation: this.rapprochementForm.value.observation,
        libelle: this.rapprochementForm.value.libelle,
        tauxTva: saisieTva.tauxTva,
        montantTva: saisieTva.montantTva,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;
      this.rapprochementService.fraisBancaireToRapprochement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Frai Bancaire validé avec succès.',
          });
          this.isSubmitting = false;
          this.displayModal = false;
          this.table.reset(); // recharge ton tableau existant
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
      this.table.reset()
    }
  }

  exportPdf(): void {
    if (!this.banqueId) {
      return;
    }
    this.exporting = true;
    const searchCriteria: FraisBancaireSearchDto = this.searchForm.getRawValue();
    this.fraisBancaireService.exportOutstandingFraisBancairePdf(this.banqueId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `frais_bancaires_encours_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
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
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }
}
