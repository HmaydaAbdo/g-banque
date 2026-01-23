
import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from "primeng/inputtext";
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { InputSwitchModule } from 'primeng/inputswitch';
import {VirementErroneResponseDto} from "../../dtos/VirementErroneResponseDto";
import {VirementErroneSearchDto} from "../../dtos/VirementErroneSearchDto";
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {VirementErroneService} from "../../service/virement-errone.service";
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {AuthService} from "../../../../../auth/services/auth.service";
import {Status} from "../../../../prefinancements/enums/Status";
import {PageResponse} from "../../../../global/types/PageResponse";



@Component({
  selector: 'app-virement-errone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule, TooltipModule, RouterLink,
    PrimeTemplate, DatePipe, DecimalPipe, NgClass, NgFor, NgIf, InputTextModule,
    TagModule, ToolbarModule, InputSwitchModule
  ],
  templateUrl: './virement-errone-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class VirementErroneListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  virements: VirementErroneResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  searchCriteria: VirementErroneSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] = [];

  constructor(
    private virementErroneService: VirementErroneService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-exclamation-triangle', route: '/portail/Virements/errones', label: 'Virements Erronés' },
    ];
    this.loadStatusOptions();
  }

  loadVirements(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.virementErroneService.searchVirementErrone(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<VirementErroneResponseDto>) => {
          this.virements = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les virements erronés.'
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

  deleteVirement(virement: VirementErroneResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le virement erroné "${virement.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.virementErroneService.deleteVirementErrone(virement.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Virement erroné supprimé.'
            });
            this.loadVirements(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Impossible de supprimer le virement erroné.'
            });
          }
        });
      }
    });
  }

  toggleVirementStatus(virement: VirementErroneResponseDto) {
    const message = virement.status === Status.PAYE
      ? 'Voulez-vous marquer ce virement erroné comme prévu ?'
      : 'Voulez-vous marquer ce virement erroné comme payé ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.virementErroneService.toggleStatus(virement.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: `Statut modifié.`
            });
            this.loadVirements(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Impossible de modifier le statut.'
            });
            this.loadVirements(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadVirements(this.table.createLazyLoadMetadata());
      }
    });
  }

  getStatusSeverity(status: Status): 'success' | 'danger' | 'warning' | 'info' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  protected readonly Status = Status;
}
