import { Component, OnInit, ViewChild, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import { ConfirmationService, MessageService, SelectItem } from 'primeng/api';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';
import { PaginatorModule } from 'primeng/paginator';
import { DividerModule } from 'primeng/divider';

import {EncoursCMTResponseDTO} from "../../../../../cmt/dtos/EncoursCMTResponseDTO";
import {EcheanceCmtService} from "../../../../../cmt/services/EcheanceCmtService";
import {EncoursCMTRequestDTO} from "../../../../../cmt/dtos/EncoursCMTRequestDTO";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";
import {InputTextareaModule} from "primeng/inputtextarea";
import {ValidationService} from "../../../../services/validation/validation.service";
import {DonneesComplementairesRapprochementDTO} from "../../../../dtos/DonneesComplementairesRapprochementDTO";
import {CmtTransactionRequest} from "../../../../dtos/cmt/CmtTransactionRequest";
import {EcheanceCMTResponseDto} from "../../../../../cmt/dtos/EcheanceCMTResponseDto";
import { InputTextModule } from 'primeng/inputtext';
import {BankStateService} from "../../../../../comptes-bancaire/services/bank-state.service";
import { parseLocalDate } from '../../../../../global/types/date.utils';


@Component({
  selector: 'app-echeances-cmt',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ConfirmDialogModule,
    RippleModule,
    PaginatorModule,
    DividerModule,
    DatePipe,
    CalendarModule,
    DialogModule,
    InputTextareaModule,
    InputTextModule
  ],
  templateUrl: './echeances-cmt.component.html',
  styleUrl: './echeances-cmt.component.scss',
  providers: [MessageService, ConfirmationService,DecimalPipe]
})
export class EcheancesCmtComponent implements OnInit, OnDestroy {

  encoursResponse: EncoursCMTResponseDTO | null = null;
  echeances: EcheanceCMTResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  currentMonthYear: string | null = null;


  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  searchForm!: FormGroup;
  banqueIdFromRoute!: number;

  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedEcheance: EcheanceCMTResponseDto | null = null;

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];
  isSubmitting: boolean = false;

  private readonly echeanceCmtService = inject(EcheanceCmtService);
  private readonly route = inject(ActivatedRoute);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);
  private readonly rapprochementService = inject(ValidationService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly datePipe = inject(DatePipe);
  private readonly decimalPipe = inject(DecimalPipe);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit(): void {
    this.subscriptions.push(
      this.route.parent!.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueIdFromRoute = +id;
          this.initSearchForm();
          this.onSearch();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      designation: [''] ,
      montant:[null]
    });

    const formValueChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.onSearch();
      });
    this.subscriptions.push(formValueChangesSub);
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['',Validators.required],
      tvaDeductible: [null, Validators.required] ,
      libelle: ['', Validators.required]
    });
  }

  loadEncours(event: TableLazyLoadEvent): void {
    const searchCriteria = this.searchForm.value;


    this.loading = true;

    const request: EncoursCMTRequestDTO = {
      banqueId: this.banqueIdFromRoute,
      designation: searchCriteria.designation,
      montant: searchCriteria.montant,
      page: event.first! / event.rows!,
      size: event.rows!,
      sort: event.sortField && event.sortOrder
        ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
        : 'dateEcheance,asc'
    };

    this.echeanceCmtService.getEncoursCMT(request).subscribe({
      next: (response: EncoursCMTResponseDTO) => {
        this.encoursResponse = response;
        this.echeances = response.echeances.content;
        this.totalRecords = response.echeances.page.totalElements;
        this.loading = false;
        this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/yyyy');
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadEncours(this.dt.createLazyLoadMetadata());
    } else {
      this.loadEncours({
        first: 0,
        rows: 10,
        filters: {},
        sortField: 'dateEcheance',
        sortOrder: 1
      });
    }
  }

  openRapprochementModal(echeance: EcheanceCMTResponseDto): void {
    this.selectedEcheance = echeance;
    this.initRapprochementForm();
    this.selectedEcheance = echeance;
    this.initRapprochementForm();



    this.rapprochementForm.patchValue({
      // Pre-fill dateEmission with dateEcheance (convert string to Date object)
      dateEmission: parseLocalDate(echeance.dateEcheance)!,

      // Pre-fill modifiable text fields
      observation: echeance.observation,
      libelle: echeance.libelle,

      // dateValeur and dateOperation remain null for user entry
    });

    // 2. Disabling controls
    this.rapprochementForm.get('dateEmission')?.disable();
    this.displayModal = true;
  }

  confirmRapprochement() {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.'
      });
      return;
    }

    if (this.rapprochementForm.valid && this.selectedEcheance) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés
      const formValue = this.rapprochementForm.getRawValue();
      const selectedEcheance = this.selectedEcheance;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');

      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher cette échéance CMT ?
            <br><br>
            <strong>Désignation du crédit:</strong> ${selectedEcheance.credit.designation}
            <br>
            <strong>Montant TTC:</strong> ${this.decimalPipe.transform(selectedEcheance.montantTtc,"1.2-2",'fr-FR')} MAD
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
        header: 'Confirmation de Rapprochement CMT',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.executeRapprochement(); // Appel de la méthode d'exécution
        },
        reject: () => {
          // L'utilisateur a annulé
        }
      });
    }
  }

  /**
   * Exécute l'appel API réel après la confirmation de l'utilisateur.
   * C'est l'ancienne méthode submitRapprochement, renommée pour clarifier son rôle.
   */
  private executeRapprochement(): void {
    // Vérification redondante, mais garantit que les données sont prêtes
    if (this.rapprochementForm.valid && this.selectedEcheance) {
      // Utilisation de getRawValue() pour récupérer aussi les champs désactivés
      const formValue = this.rapprochementForm.getRawValue();

      const donneesComplementaires: DonneesComplementairesRapprochementDTO = formValue as DonneesComplementairesRapprochementDTO;
      const rapprochementId = this.bankStateService.getRapprochementId();

      const cmtTransactionRequest: CmtTransactionRequest = {
        creditMoyenTermeId: this.selectedEcheance.credit.id,
        echeanceId: this.selectedEcheance.id,
        dateEmission: this.datePipe.transform( donneesComplementaires.dateEmission,'yyyy-MM-dd') as string,
        dateValeur: this.datePipe.transform( donneesComplementaires.dateValeur, 'yyyy-MM-dd') as string ,
        dateOperation: this.datePipe.transform(donneesComplementaires.dateOperation,'yyyy-MM-dd') as string,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        libelle:donneesComplementaires.libelle,
        rapprochementId: rapprochementId!
      };

      this.isSubmitting = true;
      this.displayModal = false;

      this.rapprochementService.echeanceCmtToRapprochement(cmtTransactionRequest)
        .subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Échéance CMT rapprochée avec succès.'
            });
            this.onSearch();
            this.isSubmitting = false;
            this.bankStateService.refreshBankInfo(this.banqueIdFromRoute);
            this.bankStateService.notifyValidation();

          },
          error: () => {
            this.isSubmitting = false;
          }
        });
    } else {
      // Devrait être capturé par confirmRapprochement, mais c'est une sécurité
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Tentative d\'exécution invalide.'
      });
    }
  }




  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }


}
