import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { RouterLink } from '@angular/router';

import { CommissionService } from '../../service/commission.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { CommissionResponseDto } from '../../dtos/commission-response.dto';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import {CommissionSearchDto} from "../../dtos/CommissionSearchDto";
import {TransactionSource} from "../../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import {Status} from "../../../prefinancements/enums/Status";
import {PageResponse} from "../../../global/types/PageResponse";
import {InputTextModule} from "primeng/inputtext";
import {AuthService} from "../../../../auth/services/auth.service";


@Component({
  selector: 'app-commission-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    BreadcrumbModule,
    ConfirmDialogModule,
    ToastModule,
    TooltipModule,
    RouterLink,
    PrimeTemplate,
    DatePipe,
    DecimalPipe,
    NgClass,
    NgFor,
    NgIf,
    InputTextModule
  ],
  templateUrl: './commission-list.component.html',

  providers: [MessageService, ConfirmationService]
})
export class CommissionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  commissions: CommissionResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: CommissionSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedOperation?: TransactionSource;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  operationOptions: { label: string; value: TransactionSource | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private commissionService: CommissionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/commissions', label: 'Commissions' },
    ];
    this.loadOperationOptions();
    this.loadStatusOptions();
  }

  loadCommissions(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.operation = this.selectedOperation;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.commissionService.searchCommissions(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<CommissionResponseDto>) => {
          this.commissions = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les commissions.'
          });
          this.loading = false;
        }
      });
  }

  loadBanquesLazy(event: any) {
    this.banquesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    this.banqueService.getAllBanques({}, page, size, 'nom,asc')
      .subscribe({
        next: res => {
          this.banques = res.content;
          this.banquesTotal = res.page.totalElements;
          this.banquesLoading = false;
        },
        error: () => this.banquesLoading = false
      });
  }

  loadOperationOptions() {
    this.operationOptions = Object.values(TransactionSource).map(op => ({
      label: op,
      value: op
    }));
  }

  loadStatusOptions() {
    this.statusOptions = Object.values(Status).map(s => ({
      label: s,
      value: s
    }));
  }

  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters() {
    this.selectedBanque = undefined;
    this.selectedOperation = undefined;
    this.selectedStatus = undefined;
    this.libelleFilter = null;
    this.applyFilters();
  }

  deleteCommission(commission: CommissionResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la commission "${commission.libelle}" ?`,
      accept: () => {
        this.commissionService.deleteCommission(commission.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Commission supprimée.' });
            this.loadCommissions(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer la commission.' });
          }
        });
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}
