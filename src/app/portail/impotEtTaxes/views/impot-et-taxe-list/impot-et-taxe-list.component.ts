import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
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
import { InputTextModule } from "primeng/inputtext";
import { AuthService } from "../../../../auth/services/auth.service";
import { Motif, MotifLabels } from "../../dtos/Motif";
import { ImpotEtTaxeService } from "../../service/ImpotEtTaxeService";
import { ToolbarModule } from "primeng/toolbar";
import { TagModule } from "primeng/tag";
import { InputSwitchModule } from "primeng/inputswitch";

@Component({
  selector: 'app-impot-et-taxe-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    // FormsModule conservé uniquement pour le [ngModel] du p-inputSwitch PREVU/PAYE
    // (exception établie, voir cmt/tableau-amortissement-cmt).
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
    InputTextModule,
    ToolbarModule,
    TagModule,
    InputSwitchModule
  ],
  templateUrl: './impot-et-taxe-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class ImpotEtTaxeListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  private readonly impotEtTaxeService = inject(ImpotEtTaxeService);
  private readonly banqueService = inject(BanqueService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  impotEtTaxes: ImpotEtTaxeResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  // Filtres (Reactive Forms)
  filterForm: FormGroup = this.fb.group({
    banque: [null],
    motif: [null],
    status: [null],
    libelle: [null]
  });

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  motifOptions: { label: string; value: Motif | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  // Breadcrumb items
  items: MenuItem[] = [];

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

    this.impotEtTaxeService.searchImpotEtTaxes(this.buildSearchDto(), page, size, sort)
      .subscribe({
        next: res => {
          this.impotEtTaxes = res.page.content;
          this.totalRecords = res.page.page.totalElements;
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
    this.filterForm.reset();
    this.applyFilters();
  }

  /** Critères de filtrage courants (sans pagination) — partagés entre la recherche et l'export PDF. */
  private buildSearchDto(): ImpotEtTaxeSearchDto {
    const formValue = this.filterForm.value;
    return {
      banqueId: formValue.banque?.id,
      motif: formValue.motif ?? undefined,
      status: formValue.status ?? undefined,
      libelle: formValue.libelle ? formValue.libelle : undefined
    };
  }

  /**
   * Exporte en PDF l'intégralité des impôts/taxes correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    this.impotEtTaxeService.exportPdf(this.buildSearchDto()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `impots_taxes_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  /**
   * Échappe les caractères HTML spéciaux avant interpolation dans un message de confirmation
   * PrimeNG (rendu via [innerHTML] par p-confirmDialog) — le libellé est une donnée libre saisie
   * par un utilisateur Finance et ne doit jamais être insérée telle quelle dans du HTML
   * (Frontend.md, section Sécurité).
   */
  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  deleteImpotEtTaxe(impotEtTaxe: ImpotEtTaxeResponseDto) {
    const libelleEchappe = this.escapeHtml(impotEtTaxe.libelle);
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'impôt/taxe "${libelleEchappe}" ?`,
      accept: () => {
        this.impotEtTaxeService.deleteImpotEtTaxe(impotEtTaxe.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impôt/taxe supprimé(e).' });
            this.loadImpotEtTaxes(this.table.createLazyLoadMetadata());
          }
        });
      }
    });
  }

  toggleStatus(impotEtTaxe: ImpotEtTaxeResponseDto) {
    const message = impotEtTaxe.status === Status.PAYE
      ? 'Voulez-vous marquer cet impôt comme prévu ?'
      : 'Voulez-vous marquer cet impôt comme payé ?';

    this.confirmationService.confirm({
      message: message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.impotEtTaxeService.toggleStatus(impotEtTaxe.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Statut modifié.` });
            this.loadImpotEtTaxes(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.loadImpotEtTaxes(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadImpotEtTaxes(this.table.createLazyLoadMetadata());
      }
    });
  }

  getStatusSeverity(status: Status): 'success' | 'danger' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  protected readonly Status = Status;
}
