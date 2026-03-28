import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { InputSwitchModule } from 'primeng/inputswitch';
import {ImpayeeResponseDto} from "../../dtos/ImpayeeResponseDto";
import {ImpayeeSearchDto} from "../../dtos/ImpayeeSearchDto";
import {TransactionSource, TransactionSourceLabels} from "../../../transactions/dtos/TransactionSource";
import {ImpayeeService} from "../../service/ImpayeeService";
import { Status } from "../../../prefinancements/enums/Status";
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from "../../../../auth/services/auth.service";
import { PageResponse } from "../../../global/types/PageResponse";
import {Type} from "../../../Virements/Type";


@Component({
  selector: 'app-impayee-list',
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
    NgIf,
    InputTextModule,
    ToolbarModule,
    TagModule,
    InputSwitchModule
  ],
  templateUrl: './Impayee-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class ImpayeeListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  impayees: ImpayeeResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: ImpayeeSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedMotif?: TransactionSource;
  selectedType?: Type;
  selectedStatus?: Status;
  libelleFilter: string | null = null;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  motifOptions: { label: string; value: TransactionSource | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];
  typeOptions: { label: string; value: Type | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private impayeeService: ImpayeeService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/impayees', label: 'Impayées' },
    ];
    this.loadMotifOptions();
    this.loadStatusOptions();
    this.loadTypeOptions()
  }

  loadImpayees(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.motif = this.selectedMotif;
    this.searchCriteria.type = this.selectedType;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.libelle = this.libelleFilter ? this.libelleFilter : undefined;

    this.impayeeService.searchImpayees(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<ImpayeeResponseDto>) => {
          this.impayees = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les impayées.'
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
    this.motifOptions = Object.values(TransactionSource).map(motif => ({
      label: TransactionSourceLabels[motif],
      value: motif
    }));
  }

  loadTypeOptions(): void {
    this.typeOptions = Object.values(Type).map(type => ({
      label: type,
      value: type
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
    this.selectedType = undefined;
    this.selectedStatus = undefined;
    this.libelleFilter = null;
    this.applyFilters();
  }

  deleteImpayee(impayee: ImpayeeResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'impayée "${impayee.libelle}" ?`,
      accept: () => {
        this.impayeeService.deleteImpayee(impayee.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impayée supprimée.' });
            this.loadImpayees(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer l\'impayée.' });
          }
        });
      }
    });
  }

  toggleStatus(impayee: ImpayeeResponseDto) {
    const message = impayee.status === Status.PAYE
      ? 'Voulez-vous marquer cette impayée comme prévue ?'
      : 'Voulez-vous marquer cette impayée comme payée ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.impayeeService.toggleStatus(impayee.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Statut modifié.` });
            this.loadImpayees(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec de modification.' });
            this.loadImpayees(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadImpayees(this.table.createLazyLoadMetadata());
      }
    });
  }

  getMotifLabel(motif: TransactionSource ): string {
    return TransactionSourceLabels[motif];
  }

  getStatusSeverity(status: Status): 'success' | 'danger' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  getTypeSeverity(type: Type): 'success' | 'danger' {
    return type === Type.ENCAISSEMENT ? 'success' : 'danger';
  }

  protected readonly Status = Status;

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }


}
