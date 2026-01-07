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

import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { ImpotEtTaxeResponseDto } from '../../dtos/ImpotEtTaxeResponseDto';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { ImpotEtTaxeSearchDto } from "../../dtos/ImpotEtTaxeSearchDto";
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { InputTextModule } from "primeng/inputtext";
import { AuthService } from "../../../../auth/services/auth.service";
import {Motif, MotifLabels} from "../../dtos/Motif";
import {ImpotEtTaxeService} from "../../service/ImpotEtTaxeService";

@Component({
  selector: 'app-impot-et-taxe-list',
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
  templateUrl: './impot-et-taxe-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class ImpotEtTaxeListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  impotEtTaxes: ImpotEtTaxeResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: ImpotEtTaxeSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedMotif?: Motif;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  motifOptions: { label: string; value: Motif | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

  // Expose MotifLabels to template
  motifLabels:Record<Motif, string> = MotifLabels;

  constructor(
    private impotEtTaxeService: ImpotEtTaxeService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/impots-taxes', label: 'Impôts et Taxes' },
    ];
    this.loadMotifOptions();
    this.loadStatusOptions();
  }

  loadImpotEtTaxes(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.motif = this.selectedMotif;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.impotEtTaxeService.searchImpotEtTaxes(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<ImpotEtTaxeResponseDto>) => {
          this.impotEtTaxes = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les impôts et taxes.'
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

  loadMotifOptions() {
    this.motifOptions = Object.values(Motif).map(motif => ({
      label: MotifLabels[motif],
      value: motif
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
    this.selectedMotif = undefined;
    this.selectedStatus = undefined;
    this.libelleFilter = null;
    this.applyFilters();
  }

  deleteImpotEtTaxe(impotEtTaxe: ImpotEtTaxeResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'impôt/taxe "${impotEtTaxe.libelle}" ?`,
      accept: () => {
        this.impotEtTaxeService.deleteImpotEtTaxe(impotEtTaxe.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impôt/taxe supprimé(e).' });
            this.loadImpotEtTaxes(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer l\'impôt/taxe.' });
          }
        });
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}
