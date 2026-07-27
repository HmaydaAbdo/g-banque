import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { CalendarModule } from 'primeng/calendar';

import { FraisBancaireResponseDto } from "../../dtos/FraisBancaireResponseDto";
import { FraisBancaireSearchDto } from "../../dtos/FraisBancaireSearchDto";
import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { Status } from "../../../prefinancements/enums/Status";
import { FraisBancaireService } from "../../service/FraisBancaireService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { PageResponse } from "../../../global/types/PageResponse";
import { AuthService } from "../../../../auth/services/auth.service";

@Component({
  selector: 'app-frais-bancaire-list',
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
    NgFor,
    NgIf,
    InputTextModule,
    TagModule,
    ToolbarModule,
    InputSwitchModule,
    CalendarModule
  ],
  templateUrl: './frais-bancaire-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class FraisBancaireListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  fraisBancaires: FraisBancaireResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  // Filters
  searchCriteria: FraisBancaireSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    status: [null as Status | null],
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

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private fraisBancaireService: FraisBancaireService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-wallet', route: '/portail/frais-bancaire', label: 'Frais Bancaires' },
    ];
    this.loadStatusOptions();
  }

  loadFraisBancaires(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const { banque, status, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle || undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.fraisBancaireService.searchFraisBancaire(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<FraisBancaireResponseDto>) => {
          this.fraisBancaires = res.content;
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

  exportPdf(): void {
    this.exporting = true;
    const { banque, status, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle || undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.fraisBancaireService.exportFraisBancairePdf(this.searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `frais_bancaires_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      }
    });
  }

  deleteFraisBancaire(frais: FraisBancaireResponseDto) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le frais bancaire "${frais.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.fraisBancaireService.deleteFraisBancaire(frais.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Frais bancaire supprimé.'
            });
            this.loadFraisBancaires(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la suppression du frais bancaire:', err);
          }
        });
      }
    });
  }

  /**
   * Toggle le statut d'un frais bancaire (PREVU <-> REALISE)
   */
  toggleFraisBancaireStatus(frais: FraisBancaireResponseDto) {
    const message = frais.status === Status.PAYE
      ? 'Voulez-vous marquer ce frais bancaire comme prévu ?'
      : 'Voulez-vous marquer ce frais bancaire comme réalisé ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.fraisBancaireService.toggleStatus(frais.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: `Statut modifié.`
            });
            this.loadFraisBancaires(this.table.createLazyLoadMetadata());
          },
          error: (err) => {
            console.error('Erreur lors de la modification du statut:', err);
            this.loadFraisBancaires(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadFraisBancaires(this.table.createLazyLoadMetadata());
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

  protected readonly Status = Status;
}
