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
import {RippleModule} from 'primeng/ripple';
import {VirementResponseDto} from "../../../../../Virements/dtos/VirementResponseDto";
import {VirementService} from "../../../../../Virements/service/VirementService";
import {PageResponse} from "../../../../../global/types/PageResponse";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {VirementTransactionRequestDto} from "../../../../rapprochement/dtos/virements/VirementTransactionRequestDto";
import {
  DonneesComplementairesRapprochementDTO
} from "../../../../rapprochement/dtos/DonneesComplementairesRapprochementDTO";
import {DividerModule} from "primeng/divider";
import {InputTextareaModule} from "primeng/inputtextarea";
import {CalendarModule} from "primeng/calendar";
import {DialogModule} from "primeng/dialog";



@Component({
  selector: 'app-virement-encours',
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
  ],
  templateUrl: './virement-encours.component.html',
  styleUrl: './virement-encours.component.scss',
  providers: [MessageService],
})
export class VirementEncoursComponent implements OnInit {
  virements: VirementResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  banqueIdFromRoute!: number;
  totalEncours: number = 0;
  currentMonthYear: string | null = null;
  @ViewChild('dt') dt: Table | undefined;

  // Propriétés pour la modale de rapprochement
  displayModal: boolean = false;
  rapprochementForm!: FormGroup;
  selectedVirement: VirementResponseDto | null = null;

  constructor(
    private virementService: VirementService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    private datePipe: DatePipe,
    private rapprochementService: RapprochemntService,
    private fb: FormBuilder,
  ) {
  }

  ngOnInit(): void {
    // Get the bank ID from the route's parent parameter
    this.route.parent!.paramMap.subscribe((params) => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueIdFromRoute = +id;
        this.loadOutstandingVirements();
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

  openRapprochementModal(virement: VirementResponseDto): void {
    this.selectedVirement = virement;
    this.initRapprochementForm();
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

    if (this.rapprochementForm.valid && this.selectedVirement) {
      const donneesComplementaires: DonneesComplementairesRapprochementDTO = this.rapprochementForm.value;

      const virementTransactionRequest: VirementTransactionRequestDto = {
        numeroVirement: this.selectedVirement.numero,
        dateEmission: donneesComplementaires.dateEmission,
        dateValeur: donneesComplementaires.dateValeur,
        dateOperation: donneesComplementaires.dateOperation,
        observation: donneesComplementaires.observation,
        tvaDeductible: donneesComplementaires.tvaDeductible,
        transactionSource: TransactionSource.Virement
      };

      this.rapprochementService.virementToRapprochement(virementTransactionRequest)
        .subscribe({
          next: (response) => {
            console.log(response);
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Virement rapproché avec succès.'
            });
            this.loadOutstandingVirements();
            this.displayModal = false;
          },
          error: (error) => {
            console.error('Erreur lors du rapprochement du virement:', error);
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
   * Loads the outstanding virements for the bank.
   * @param event The lazy load event from the PrimeNG table.
   */
  loadOutstandingVirements(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? event.first! / event.rows! : 0;
    const size = event ? event.rows! : 10;
    const sort = event?.sortField
      ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`
      : 'dateEmission,desc';

    this.virementService
      .getOutstandingVirementsByBank(this.banqueIdFromRoute, page, size, sort)
      .subscribe({
        next: (response: PageResponse<VirementResponseDto>) => {
          this.virements = response.content;
          this.totalRecords = response.page.totalElements;
          this.loading = false;
          // Calculate the total outstanding amount
          this.totalEncours = this.virements.reduce(
            (sum, virement) => sum + virement.montant,
            0
          );
          this.currentMonthYear = this.datePipe.transform(new Date(), 'MM/YYYY');
        },
        error: (error: Error) => {
          this.loading = false;
          console.error(
            'Erreur lors du chargement des virements en cours:',
            error
          );
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des virements en cours.',
          });
        },
      });
  }
}
