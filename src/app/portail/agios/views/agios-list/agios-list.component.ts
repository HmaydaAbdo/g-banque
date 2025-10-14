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

// Agios specific imports
import { AgiosService } from '../../service/agios.service';
import { AgiosResponseDto } from '../../dtos/AgiosResponseDto';
import { AgiosSearchDto } from "../../dtos/AgiosSearchDto";

// Shared imports
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { TransactionSource } from "../../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { InputTextModule } from "primeng/inputtext";


@Component({
  selector: 'app-agios-list',
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
  templateUrl: './agios-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  agios: AgiosResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: AgiosSearchDto = {};
  selectedBanque?: BanqueResponse;
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
    private agiosService: AgiosService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/agios', label: 'Agios' },
    ];
    this.loadStatusOptions();
  }

  loadAgios(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.agiosService.searchAgios(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<AgiosResponseDto>) => {
          this.agios = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les agios.'
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



  loadStatusOptions() {
    this.statusOptions = Object.values(Status).map(s => ({
      label: s,
      value: s
    }));
  }

  applyFilters() {
    if (this.table) {
      // Use a setTimeout to ensure PrimeNG's change detection cycle correctly handles the reset.
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters() {
    this.selectedBanque = undefined;
    this.selectedStatus = undefined;
    this.libelleFilter = null;
    this.applyFilters();
  }

  deleteAgios(agios: AgiosResponseDto) { // Updated method name and DTO
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'agio " ?`,
      accept: () => {
        this.agiosService.deleteAgios(agios.id).subscribe({ // Updated service call
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Agio supprimé.' }); // Updated message
            this.loadAgios(this.table.createLazyLoadMetadata()); // Updated method call
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer l\'agio.' }); // Updated message
          }
        });
      }
    });
  }
}
