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

import { OperationErroneResponseDto } from "../../dtos/OperationErroneResponseDto";
import { OperationErroneSearchDto } from "../../dtos/OperationErroneSearchDto";
import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { OperationErroneService } from "../../service/operation-errone.service";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { AuthService } from "../../../../auth/services/auth.service";
import { Status } from "../../../prefinancements/enums/Status";
import { PageResponse } from "../../../global/types/PageResponse";
import { TransactionSource } from "../../../transactions/dtos/TransactionSource";

@Component({
  selector: 'app-operation-errone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule, TooltipModule, RouterLink,
    PrimeTemplate, DatePipe, DecimalPipe, NgClass, NgFor, NgIf, InputTextModule,
    TagModule, ToolbarModule, InputSwitchModule, CalendarModule
  ],
  templateUrl: './operation-errone-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class OperationErroneListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  operations: OperationErroneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  searchCriteria: OperationErroneSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    status: [null as Status | null],
    motif: [null as TransactionSource | null],
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

  items: MenuItem[] = [];

  constructor(
    private operationErroneService: OperationErroneService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-exclamation-triangle', route: '/portail/operations/errones', label: 'Opérations Erronées' },
    ];
    this.loadStatusOptions();
    this.loadMotifOptions();
  }

  loadOperations(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const { banque, status, motif, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.operationErroneService.searchOperationErrone(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<OperationErroneResponseDto>) => {
          this.operations = res.content;
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
   * Exporte en PDF l'intégralité des opérations erronées correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const { banque, status, motif, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.operationErroneService.exportPdf(this.searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `operations_erronees_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteOperation(operation: OperationErroneResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'opération erronée "${operation.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.operationErroneService.deleteOperationErrone(operation.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Opération erronée supprimée.'
            });
            this.loadOperations(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la suppression de l\'opération erronée:', err);
          }
        });
      }
    });
  }

  toggleOperationStatus(operation: OperationErroneResponseDto) {
    const message = operation.status === Status.PAYE
      ? 'Voulez-vous marquer cette opération erronée comme prévue ?'
      : 'Voulez-vous marquer cette opération erronée comme payée ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.operationErroneService.toggleStatus(operation.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: `Statut modifié.`
            });
            this.loadOperations(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la modification du statut:', err);
            this.loadOperations(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadOperations(this.table.createLazyLoadMetadata());
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
