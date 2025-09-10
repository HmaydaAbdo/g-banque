import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CommonModule, DatePipe} from '@angular/common';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {PaginatorModule} from 'primeng/paginator';
import {MessageService} from 'primeng/api';
import {PageResponse} from "../../../../../global/types/PageResponse";
import {ChequeResponseDto} from "../../../../../cheques/dtos/ChequeResponseDto";
import {ChequeService} from "../../../../../cheques/services/ChequeService";
import {Ripple} from "primeng/ripple";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {DonneesComplementairesRapprochementDTO} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {ChequeTransactionRequest} from "../../../../rapprochement/dtos/cheques/ChequeTransactionRequest";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {DividerModule} from "primeng/divider";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";


@Component({
  selector: 'app-cheques-encours',
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
    Ripple,
    DividerModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule,
  ],
  templateUrl: './cheques-encours.component.html',
  styleUrl: './cheques-encours.component.scss',
  providers: [MessageService],
})
export class ChequesEncoursComponent implements OnInit {
  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours: number = 0;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Propriétés pour la modale de rapprochement
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedCheque: ChequeResponseDto | null = null;

  constructor(
    private chequeService: ChequeService,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe
  ) {}

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingCheques();
      }
    });
    this.initRapprochementForm();
  }

  initRapprochementForm(): void {
    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperation: [null, Validators.required],
      observation: ['', Validators.required],
      tvaDeductible: [0, [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  openRapprochementModal(cheque: ChequeResponseDto): void {
    this.selectedCheque = cheque;
    this.initRapprochementForm(); // Reset form on open
    this.displayModal = true;
  }

  submitRapprochement(): void {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.'
      });
      return;
    }

    if (this.rapprochementForm.valid && this.selectedCheque) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      const chequeTransactionRequest: ChequeTransactionRequest = {
        numeroCheque: this.selectedCheque.numero,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.CHEQUE
      };

      this.rapprochementService.chequeToRapprochement(chequeTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Chèque rapproché avec succès.'
            });
            this.loadOutstandingCheques(); // Refresh the table
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement du chèque:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: error.error.message || 'Une erreur est survenue lors du rapprochement.'
            });
          }
        });
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
  /**
   * Loads the outstanding cheques for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingCheques(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.chequeService
      .getOutstandingChequesByBank(this.banqueIdFromRoute, page, size, sort)
      .subscribe({
        next: (response: PageResponse<ChequeResponseDto>) => {
          this.cheques = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = this.cheques.reduce(
            (sum, cheque) => sum + cheque.montant,
            0
          );
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error:Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des chèques en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des chèques en cours.',
          });
        },
      });
  }
}
