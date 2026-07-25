import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { PrelevementResponseDto } from "../../dtos/PrelevementResponseDto";
import { PrelevementSearchDto } from "../../dtos/PrelevementSearchDto";
import { PrelevementService } from "../../service/prelevement.service";
import { AuthService } from "../../../../auth/services/auth.service";

@Component({
  selector: 'app-prelevement-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
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
    InputTextModule,
    TagModule,
    ToolbarModule,
    InputSwitchModule
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
  exporting = false;

  // Filtres
  searchCriteria: PrelevementSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    status: [null as Status | null],
    libelle: [null as string | null],
  });

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
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private fb: FormBuilder
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

    const { banque, status, libelle } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;

    this.prelevementService.searchPrelevements(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<PrelevementResponseDto>) => {
          this.prelevements = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
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

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  deletePrelevement(prelevement: PrelevementResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le prélèvement "${prelevement.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.prelevementService.deletePrelevement(prelevement.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Prélèvement supprimé.'
            });
            this.loadPrelevements(this.table.createLazyLoadMetadata());
          }
        });
      }
    });
  }

  /**
   * Toggle le statut d'un prélèvement (PREVU <-> REALISE)
   */
  togglePrelevementStatus(prelevement: PrelevementResponseDto) {
    const message = prelevement.status === Status.PAYE
      ? 'Voulez-vous marquer ce prélèvement comme prévu ?'
      : 'Voulez-vous marquer ce prélèvement comme payé ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.prelevementService.toggleStatus(prelevement.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: `Statut modifié.`
            });
            this.loadPrelevements(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.loadPrelevements(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadPrelevements(this.table.createLazyLoadMetadata());
      }
    });
  }

  /**
   * Retourne la couleur du tag en fonction du statut
   */
  getStatusSeverity(status: Status): 'success' | 'danger' | 'warning' | 'info' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  exportPdf(): void {
    this.exporting = true;
    const { banque, status, libelle } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;

    this.prelevementService.exportPdf(this.searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `prelevements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  protected readonly Status = Status;
}
