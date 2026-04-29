import {Component, OnInit, ViewChild} from '@angular/core';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from 'primeng/api';
import {CommonModule, DatePipe, DecimalPipe, NgClass, NgFor, NgIf} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {ToastModule} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {RouterLink} from '@angular/router';

import {CommissionService} from '../../service/commission.service';
import {BanqueService} from '../../../comptes-bancaire/services/BanqueService';
import {CommissionResponseDto} from '../../dtos/commission-response.dto';
import {BanqueResponse} from '../../../comptes-bancaire/dtos/BanqueResponse';
import {CommissionSearchDto} from '../../dtos/CommissionSearchDto';
import {TransactionSource} from '../../../transactions/dtos/TransactionSource';
import {Status} from '../../../prefinancements/enums/Status';
import {PageResponse} from '../../../global/types/PageResponse';
import {InputTextModule} from 'primeng/inputtext';
import {AuthService} from '../../../../auth/services/auth.service';
import {TagModule} from 'primeng/tag';
import {ToolbarModule} from 'primeng/toolbar';
import {InputSwitchModule} from 'primeng/inputswitch';

@Component({
  selector: 'app-commission-list',
  standalone: true,
  imports: [
    CommonModule,
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
    FormsModule
  ],
  templateUrl: './commission-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class CommissionListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  commissions: CommissionResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  filterForm!: FormGroup;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;
  operationOptions: { label: string; value: TransactionSource | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] = [];

  constructor(
    private fb: FormBuilder,
    private commissionService: CommissionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/commissions', label: 'Commissions'},
    ];

    this.filterForm = this.fb.group({
      banqueId: [null],
      operation: [null],
      status: [null],
      libelle: [null],
      montant: [null]
    });

    this.loadOperationOptions();
    this.loadStatusOptions();
  }

  get searchCriteria(): CommissionSearchDto {
    return  this.filterForm.value as CommissionSearchDto;
  }

  loadCommissions(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.commissionService.searchCommissions(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<CommissionResponseDto>) => {
          this.commissions = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les commissions.'
          });
          this.loading = false;
        }
      });
  }

  loadBanquesLazy(event: any): void {
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

  loadOperationOptions(): void {
    this.operationOptions = Object.values(TransactionSource).map(op => ({
      label: op,
      value: op
    }));
  }

  loadStatusOptions(): void {
    this.statusOptions = Object.values(Status).map(s => ({
      label: s,
      value: s
    }));
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

  deleteCommission(commission: CommissionResponseDto): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la commission "${commission.libelle}" ?`,
      accept: () => {
        this.commissionService.deleteCommission(commission.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Commission supprimée.'});
            this.loadCommissions(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer la commission.'});
          }
        });
      }
    });
  }

  toggleCommissionStatus(commission: CommissionResponseDto): void {
    const message = commission.status === Status.PAYE
      ? 'Voulez-vous marquer cette commission comme prévu ?'
      : 'Voulez-vous marquer cette commission comme payée ?';

    this.confirmationService.confirm({
      message,
      header: 'Confirmation',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.commissionService.toggleCommissionStatus(commission.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Statut modifié.'});
            this.loadCommissions(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Impossible de modifier le statut de la commission.'});
            this.loadCommissions(this.table.createLazyLoadMetadata());
          }
        });
      },
      reject: () => {
        this.loadCommissions(this.table.createLazyLoadMetadata());
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
