import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, MenuItem } from 'primeng/api';
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
import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";
import {CommissionService} from "../../../../../commission/service/commission.service";
import {CommissionSearchDto} from "../../../../../commission/dtos/CommissionSearchDto";
import {EncoursCommissionDto} from "../../../../../commission/dtos/EncoursCommissionDto";
import {CommissionValidationDto} from "../../../../rapprochement/dtos/commission/CommissionValidationDto";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";

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
    InputTextareaModule
  ],
  templateUrl: './encours-commissions.component.html',
  providers: [MessageService]
})
export class EncoursCommissionsComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  commissions: CommissionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: Decimal;
  banqueId?: number;

  displayModal: boolean = false;
  selectedCommission: any = null;
  rapprochementForm!: FormGroup;

  selectedOperation?: TransactionSource;
  libelleFilter: string | null = null;
  operationOptions: { label: string; value: TransactionSource | null }[] = [];
  items: MenuItem[] = [];

  constructor(
    private commissionService: CommissionService,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: RapprochemntService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/commissions', label: 'Commissions' },
      { label: 'Encours' }
    ];

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
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
    });
  }

  loadCommissions(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.commissions = [];
      this.totalRecords = 0;
      this.totalEncours = new Decimal(0);
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: CommissionSearchDto = {
      operation: this.selectedOperation,
      libelle: this.libelleFilter || undefined
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
    this.displayModal = true;
  }



  // Soumettre la validation
  submitRapprochement(): void {
    if (this.rapprochementForm.invalid || !this.selectedCommission) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs.',
      });
      return;
    }

    const request: CommissionValidationDto = {
      idCommission: this.selectedCommission.id,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateValeur: this.rapprochementForm.value.dateValeur,
      observation: this.rapprochementForm.value.observation,
    };

    this.rapprochementService.commissionToRapprochement(request).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Commission validée avec succès.',
        });
        this.displayModal = false;
        this.table.reset(); // recharge ton tableau existant
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Une erreur est survenue.',
        });
      },
    });
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
}
