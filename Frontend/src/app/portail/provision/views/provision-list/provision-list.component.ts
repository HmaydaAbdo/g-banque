import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
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
import { CalendarModule } from 'primeng/calendar';

import { ProvisionResponseDto } from "../../dtos/ProvisionResponseDto";
import { ProvisionSearchDto } from "../../dtos/ProvisionSearchDto";
import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { ProvisionService } from "../../service/provision.service";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { AuthService } from "../../../../auth/services/auth.service";
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { TransactionSource } from "../../../transactions/dtos/TransactionSource";
import { SensOperation } from "../../../transactions/dtos/SensOperation";

@Component({
  selector: 'app-provision-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule, TooltipModule, RouterLink,
    PrimeTemplate, DatePipe, DecimalPipe, NgClass, NgFor, NgIf, InputTextModule,
    TagModule, ToolbarModule, InputSwitchModule, CalendarModule
  ],
  templateUrl: './provision-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class ProvisionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  provisions: ProvisionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  searchCriteria: ProvisionSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    status: [null as Status | null],
    motif: [null as TransactionSource | null],
    sens: [null as SensOperation | null],
    libelle: [null as string | null],
    dateEmissionDebut: [null as Date | null],
    dateEmissionFin: [null as Date | null],
    dateOperationDebut: [null as Date | null],
    dateOperationFin: [null as Date | null],
    dateValeurDebut: [null as Date | null],
    dateValeurFin: [null as Date | null],
  });

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.filterForm.value;
    let count = 0;
    if (v.dateEmissionDebut || v.dateEmissionFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
  }

  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];
  motifOptions: { label: string; value: TransactionSource | null }[] = [];
  sensOptions: { label: string; value: SensOperation | null }[] = [
    { label: 'Encaissement', value: SensOperation.ENCAISSEMENT },
    { label: 'Décaissement', value: SensOperation.DECAISSEMENT },
  ];

  items: MenuItem[] = [];

  constructor(
    private provisionService: ProvisionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-wallet', route: '/portail/provisions', label: 'Provisions' },
    ];
    this.loadStatusOptions();
    this.loadMotifOptions();
  }

  loadProvisions(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const { banque, status, motif, sens, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.sens = sens ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.provisionService.searchProvision(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<ProvisionResponseDto>) => {
          this.provisions = res.content;
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

  loadMotifOptions() {
    this.motifOptions = Object.values(TransactionSource).map(m => ({
      label: this.formatMotifLabel(m),
      value: m
    }));
  }

  formatMotifLabel(motif: TransactionSource): string {
    return motif.replace(/_/g, ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  formatSensLabel(sens: SensOperation): string {
    return sens === SensOperation.ENCAISSEMENT ? 'Encaissement' : 'Décaissement';
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

  /**
   * Exporte en PDF l'intégralité des provisions correspondant aux filtres actuellement appliqués
   * — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const { banque, status, motif, sens, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.sens = sens ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.provisionService.exportPdf(this.searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `provisions_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteProvision(provision: ProvisionResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la provision "${provision.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.provisionService.deleteProvision(provision.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Provision supprimée.'
            });
            this.loadProvisions(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la suppression de la provision:', err);
          }
        });
      }
    });
  }

  toggleProvisionStatus(provision: ProvisionResponseDto) {
    const message = provision.status === Status.PAYE
      ? 'Voulez-vous marquer cette provision comme prévue ?'
      : 'Voulez-vous marquer cette provision comme payée ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.provisionService.toggleStatus(provision.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: `Statut modifié.`
            });
            this.loadProvisions(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la modification du statut:', err);
            this.loadProvisions(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadProvisions(this.table.createLazyLoadMetadata());
      }
    });
  }

  getStatusSeverity(status: Status): 'success' | 'danger' | 'warning' | 'info' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  getSensSeverity(sens: SensOperation): 'success' | 'danger' {
    return sens === SensOperation.ENCAISSEMENT ? 'success' : 'danger';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  protected readonly Status = Status;
}
