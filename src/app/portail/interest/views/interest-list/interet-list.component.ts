import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

// Interet specific DTOs and Service


// Shared imports
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";

// PrimeNG imports
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from "primeng/inputtext";
import {InteretResponseDto} from "../../dtos/InteretResponseDto";
import {InteretSearchDto} from "../../dtos/InteretSearchDto";
import {InteretService} from "../../service/InteretService";


@Component({
  selector: 'app-interet-list',
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
  templateUrl: './interet-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class InteretListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  interets: InteretResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: InteretSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private interetService: InteretService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/interets', label: 'Intérêts' }, // Updated route and label
    ];
    this.loadStatusOptions();
  }

  loadInterets(event: TableLazyLoadEvent) { // Updated method name
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    // Apply filters to search criteria
    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.interetService.searchInterest(this.searchCriteria, page, size, sort) // Updated service call
      .subscribe({
        next: (res: PageResponse<InteretResponseDto>) => { // Updated DTO
          this.interets = res.content; // Updated array name
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les intérêts.' // Updated message
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
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters() {
    this.selectedBanque = undefined;
    this.selectedStatus = undefined;
    this.libelleFilter = null;
    this.applyFilters();
  }

  deleteInteret(interet: InteretResponseDto) { // Updated method name and DTO
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'intérêt "${interet.libelle}" ?`, // Updated message
      accept: () => {
        this.interetService.deleteInterest(interet.id).subscribe({ // Updated service call
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Intérêt supprimé.' }); // Updated message
            this.loadInterets(this.table.createLazyLoadMetadata()); // Updated method call
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer l\'intérêt.' }); // Updated message
          }
        });
      }
    });
  }
}
