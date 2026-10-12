import { Component, OnInit, ViewChild } from '@angular/core';
import { AVERTISSEMENT_RETOUR_EN_COURS } from '../../../global/utils/bascule-statut.utils';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, PrimeTemplate } from 'primeng/api';
import { CommonModule, DatePipe, DecimalPipe, NgClass, NgIf } from '@angular/common';
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
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { InputSwitchModule } from 'primeng/inputswitch';
import { CalendarModule } from 'primeng/calendar';
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
    NgIf,
    InputTextModule,
    ToolbarModule,
    TagModule,
    InputSwitchModule,
    CalendarModule
  ],
  templateUrl: './Impayee-list.component.html',
  providers: [MessageService, ConfirmationService]
})
export class ImpayeeListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  impayees: ImpayeeResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  exporting = false;

  // Filters
  searchCriteria: ImpayeeSearchDto = {};

  filterForm = this.fb.group({
    banque: [null as BanqueResponse | null],
    motif: [null as TransactionSource | null],
    type: [null as Type | null],
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
    private authService: AuthService,
    private fb: FormBuilder
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
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const { banque, motif, type, status, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.type = type ?? undefined;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.impayeeService.searchImpayees(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<ImpayeeResponseDto>) => {
          this.impayees = res.content;
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

  clearFilters(): void {
    this.filterForm.reset();
    this.applyFilters();
  }

  /**
   * Exporte en PDF l'intégralité des impayées correspondant aux filtres actuellement appliqués —
   * pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const { banque, motif, type, status, libelle, dateEmissionDebut, dateEmissionFin, dateOperationDebut, dateOperationFin, dateValeurDebut, dateValeurFin } = this.filterForm.getRawValue();
    this.searchCriteria.banqueId = banque?.id;
    this.searchCriteria.motif = motif ?? undefined;
    this.searchCriteria.type = type ?? undefined;
    this.searchCriteria.status = status ?? undefined;
    this.searchCriteria.libelle = libelle ? libelle : undefined;
    this.searchCriteria.dateEmissionDebut = dateEmissionDebut ?? undefined;
    this.searchCriteria.dateEmissionFin = dateEmissionFin ?? undefined;
    this.searchCriteria.dateOperationDebut = dateOperationDebut ?? undefined;
    this.searchCriteria.dateOperationFin = dateOperationFin ?? undefined;
    this.searchCriteria.dateValeurDebut = dateValeurDebut ?? undefined;
    this.searchCriteria.dateValeurFin = dateValeurFin ?? undefined;

    this.impayeeService.exportPdf(this.searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `impayees_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
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
          error: (err) => {
            console.error('Erreur lors de la suppression de l\'impayée:', err);
          }
        });
      }
    });
  }

  toggleStatus(impayee: ImpayeeResponseDto) {
    const message = impayee.status === Status.PAYE
      ? 'Voulez-vous marquer cette impayée comme prévue  ?' + ' ' + AVERTISSEMENT_RETOUR_EN_COURS
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
          error: (err) => {
            console.error('Erreur lors de la modification du statut:', err);
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
