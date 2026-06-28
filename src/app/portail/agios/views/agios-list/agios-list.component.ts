import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ConfirmationService, MenuItem, MessageService, PrimeTemplate } from 'primeng/api';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
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
import { InputTextModule } from 'primeng/inputtext';

import { AgiosService } from '../../service/agios.service';
import { AgiosResponseDto } from '../../dtos/AgiosResponseDto';
import { AgiosSearchDto } from '../../dtos/AgiosSearchDto';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Status } from '../../../prefinancements/enums/Status';
import { PageResponse } from '../../../global/types/PageResponse';
import { AuthService } from '../../../../auth/services/auth.service';

@Component({
  selector: 'app-agios-list',
  standalone: true,
  imports: [
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
  ],
  templateUrl: './agios-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class AgiosListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  private readonly agiosService       = inject(AgiosService);
  private readonly banqueService      = inject(BanqueService);
  private readonly messageService     = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly authService        = inject(AuthService);

  private readonly fb = inject(FormBuilder);

  agios: AgiosResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  filterForm = this.fb.group({
    banque:  [null as BanqueResponse | null],
    status:  [null as Status | null],
    libelle: [null as string | null],
  });

  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] = [];

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/agios', label: 'Agios' },
    ];
    this.loadStatusOptions();
  }

  loadAgios(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page      = event.first ? event.first / (event.rows ?? 10) : 0;
    const size      = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';

    const { banque, status, libelle } = this.filterForm.getRawValue();
    const searchCriteria: AgiosSearchDto = {
      banqueId: banque?.id,
      status:   status ?? undefined,
      libelle:  libelle ?? undefined,
    };

    this.agiosService.searchAgios(searchCriteria, page, size, `${sortField},${sortOrder}`)
      .subscribe({
        next: (res: PageResponse<AgiosResponseDto>) => {
          this.agios        = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading      = false;
        },
        error: () => { this.loading = false; }
      });
  }

  loadBanquesLazy(event: any): void {
    this.banquesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    this.banqueService.getAllBanques({}, page, size, 'nom,asc').subscribe({
      next: res => {
        this.banques      = res.content;
        this.banquesTotal = res.page.totalElements;
        this.banquesLoading = false;
      },
      error: () => { this.banquesLoading = false; }
    });
  }

  loadStatusOptions(): void {
    this.statusOptions = Object.values(Status).map(s => ({ label: s, value: s }));
  }

  applyFilters(): void {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  deleteAgios(agios: AgiosResponseDto): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer l'agios "${agios.libelle}" ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.agiosService.deleteAgios(agios.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Agios supprimé.' });
            this.loadAgios(this.table.createLazyLoadMetadata());
          },
          error: () => { this.loadAgios(this.table.createLazyLoadMetadata()); }
        });
      }
    });
  }

  toggleAgiosStatus(agios: AgiosResponseDto): void {
    const message = agios.status === Status.PAYE
      ? 'Voulez-vous marquer cet agios comme prévu ?'
      : 'Voulez-vous marquer cet agios comme payé ?';

    this.confirmationService.confirm({
      message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.agiosService.toggleStatus(agios.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut modifié.' });
            this.loadAgios(this.table.createLazyLoadMetadata());
          },
          error: () => { this.loadAgios(this.table.createLazyLoadMetadata()); }
        });
      },
      reject: () => { this.loadAgios(this.table.createLazyLoadMetadata()); }
    });
  }

  getStatusSeverity(status: Status): 'success' | 'danger' | 'warning' | 'info' {
    return status === Status.PAYE ? 'success' : 'danger';
  }

  isSystemAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  protected readonly Status = Status;
}
