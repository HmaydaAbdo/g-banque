import { Component, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// PrimeNG
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { InputSwitchModule } from 'primeng/inputswitch';

// App imports
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Status } from '../../../prefinancements/enums/Status';
import { PageResponse } from '../../../global/types/PageResponse';
import { RestitutionResponseDto } from '../../dtos/RestitutionResponseDto';
import { RestitutionSearchDto } from '../../dtos/RestitutionSearchDto';
import { RestitutionService } from '../../service/restitution.service';
import { AuthService } from "../../../../auth/services/auth.service";

@Component({
  selector: 'app-restitution-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, TableModule, DropdownModule, ButtonModule, RippleModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule, TooltipModule, RouterModule,
    DatePipe, DecimalPipe, InputTextModule, TagModule, ToolbarModule, InputSwitchModule
  ],
  templateUrl: './restitution-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class RestitutionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  restitutions: RestitutionResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  searchCriteria: RestitutionSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    status: [null as Status | null],
    libelle: [null as string | null],
  });

  banques: BanqueResponse[] = [];
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] = [];

  constructor(
    private restitutionService: RestitutionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/restitutions', label: 'Restitutions' },
    ];
    this.loadStatusOptions();
  }

  loadRestitutions(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const { banque, status, libelle } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle || undefined;

    this.restitutionService.searchRestitutions(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<RestitutionResponseDto>) => {
          this.restitutions = res.content;
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
    this.banqueService.getAllBanques({}, Math.floor(event.first / event.rows), event.rows, 'nom,asc')
      .subscribe({
        next: res => {
          this.banques = res.content;
          this.banquesLoading = false;
        },
        error: () => this.banquesLoading = false
      });
  }

  loadStatusOptions() {
    this.statusOptions = Object.values(Status).map(s => ({ label: s, value: s }));
  }

  applyFilters() {
    if (this.table) setTimeout(() => this.table.reset(), 0);
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  /**
   * Exporte en PDF l'intégralité des restitutions correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const { banque, status, libelle } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle || undefined;

    this.restitutionService.exportPdf(this.searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `restitutions_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteRestitution(restitution: RestitutionResponseDto) {
    this.confirmationService.confirm({
      message: `Supprimer la restitution "${restitution.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.restitutionService.deleteRestitution(restitution.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Supprimé.' });
            this.loadRestitutions(this.table.createLazyLoadMetadata());
          },
          error: (err) => console.error('Erreur lors de la suppression de la restitution:', err)
        });
      }
    });
  }

  toggleRestitutionStatus(restitution: RestitutionResponseDto) {
    this.confirmationService.confirm({
      message: 'Modifier le statut de cette restitution ?',
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        // Note: Assurez-vous que cette méthode existe dans votre service
        this.restitutionService.toggleStatus(restitution.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
            this.loadRestitutions(this.table.createLazyLoadMetadata());
          },
          error: () => this.loadRestitutions(this.table.createLazyLoadMetadata())
        });
      },
      reject: () => this.loadRestitutions(this.table.createLazyLoadMetadata())
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
