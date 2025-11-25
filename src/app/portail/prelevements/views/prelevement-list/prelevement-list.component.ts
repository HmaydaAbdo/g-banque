import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { InputTextModule } from "primeng/inputtext";
import {PrelevementResponseDto} from "../../dtos/PrelevementResponseDto";
import {PrelevementSearchDto} from "../../dtos/PrelevementSearchDto";
import {PrelevementService} from "../../service/prelevement.service";


@Component({
  selector: 'app-prelevement-list',
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
    InputTextModule
  ],
  templateUrl: './prelevement-list.component.html',
  styleUrl: './prelevement-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class PrelevementListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  prelevements: PrelevementResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filtres
  searchCriteria: PrelevementSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  // Fil d'Ariane
  items: MenuItem[] = [];

  constructor(
    private prelevementService: PrelevementService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/prelevements', label: 'Prélèvements' },
    ];
    this.loadStatusOptions();
  }

  loadPrelevements(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.prelevementService.searchPrelevements(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<PrelevementResponseDto>) => {
          this.prelevements = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les prélèvements.' // Changement de message
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

  deletePrelevement(prelevement: PrelevementResponseDto) { // Changement: deleteAgios -> deletePrelevement
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le prélèvement ?`, // Changement de message
      accept: () => {
        this.prelevementService.deletePrelevement(prelevement.id).subscribe({ // Changement de service
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Prélèvement supprimé.' }); // Changement de message
            this.loadPrelevements(this.table.createLazyLoadMetadata()); // Changement de méthode
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer le prélèvement.' }); // Changement de message
          }
        });
      }
    });
  }
}
