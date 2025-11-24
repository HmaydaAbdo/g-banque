
import { Component, OnInit, ViewChild } from '@angular/core';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {MessageService, ConfirmationService, MenuItem, PrimeTemplate} from 'primeng/api';
import { PrefinancementService } from '../../service/PrefinancementService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { PrefinancementResponseDTO } from '../../dtos/PrefinancementResponseDTO';
import { PrefinancementSearchDTO } from '../../dtos/PrefinancementSearchDTO';
import { PageResponse } from '../../../global/types/PageResponse';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { MarcheResponseDTO } from '../../../Marches/dtos/marche-response.dto';
import {NgIf, NgForOf, NgFor} from "@angular/common";
import { Type } from '../../enums/Type';
import { Status } from '../../enums/Status';
import {ButtonDirective} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {DatePipe, DecimalPipe, NgClass} from "@angular/common";
import {DropdownModule} from "primeng/dropdown";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ToastModule} from "primeng/toast";
import {FormsModule} from "@angular/forms";
import {TagModule} from "primeng/tag";
import {AuthService} from "../../../../auth/services/auth.service";
import {InputSwitchModule} from "primeng/inputswitch";
import {FactoringResponseDto} from "../../../factoring/dtos/FactoringResponseDto";

@Component({
  selector: 'app-prefinancements-list',
  standalone: true,
  templateUrl: './prefinancements-list.component.html',
  styleUrls: ['./prefinancements-list.component.scss'],
  imports: [
    PrimeTemplate,
    ButtonDirective,
    Ripple,
    DatePipe,
    DecimalPipe,
    TableModule,
    DropdownModule,
    BreadcrumbModule,
    ConfirmDialogModule,
    ToastModule,
    FormsModule,
    NgClass,
    NgIf,
    NgFor,
    NgForOf,
    TagModule,
    InputSwitchModule,
  ],
  providers: [MessageService, ConfirmationService]
})
export class PrefinancementsListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  prefinancements: PrefinancementResponseDTO[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: PrefinancementSearchDTO = {};
  selectedBanque?: BanqueResponse;
  selectedMarche?: MarcheResponseDTO;
  selectedType?: Type;
  selectedStatus?: Status;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;

  marches: MarcheResponseDTO[] = [];
  marchesTotal = 0;
  marchesLoading = false;

  typeOptions: { label: string; value: Type | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  // breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private prefinancementService: PrefinancementService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    public authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/préfinancements', label: 'Préfinancements' },
    ];
    this.loadTypes();
    this.loadStatuses();
    this.loadPrefinancements({ first: 0, rows: 10 });
  }

  /** Load prefinancements with filters */
  loadPrefinancements(event: TableLazyLoadEvent) {
    this.loading = true;

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.marcheId = this.selectedMarche?.id;
    this.searchCriteria.type = this.selectedType;
    this.searchCriteria.status = this.selectedStatus;

    this.prefinancementService.getAllPrefinancements(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<PrefinancementResponseDTO>) => {
          this.prefinancements = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les préfinancements.'
          });
          this.loading = false;
        }
      });
  }

  /** Lazy load banques for dropdown */
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

  /** Lazy load marches for dropdown */
  loadMarchesLazy(event: any) {
    this.marchesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;

    this.marcheService.getAllMarches({}, page, size, 'numero,asc')
      .subscribe({
        next: res => {
          this.marches = res.content;
          this.marchesTotal = res.page.totalElements;
          this.marchesLoading = false;
        },
        error: () => this.marchesLoading = false
      });
  }

  /** Load static type options */
  loadTypes() {
    this.typeOptions = [
      { label: 'Tous les types', value: null },
      { label: 'REMBOURSEMENT', value: Type.REMBOURSEMENT },
      { label: 'DEBLOCAGE', value: Type.DEBLOCAGE }
    ];
  }

  /** Load static status options */
  loadStatuses() {
    this.statusOptions = [
      { label: 'Tous les statuts', value: null },
      { label: 'Prévu', value: Status.PREVU },
      { label: 'Payé', value: Status.PAYE }
    ];
  }

  /** Apply filters and reload table */
  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  /** Clear all filters */
  clearFilters() {
    this.selectedBanque = undefined;
    this.selectedMarche = undefined;
    this.selectedType = undefined;
    this.selectedStatus = undefined;
    this.applyFilters();
  }

  toggleStatus(pref: PrefinancementResponseDTO) {

    this.prefinancementService.toggleStatus(pref.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
        this.loadPrefinancements(this.table.createLazyLoadMetadata());
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.' });
      }
    });
  }

  confirmToggleStatus(pref: PrefinancementResponseDTO){
    const confirmationMessage = `Êtes-vous sûr de vouloir ${pref.status==Status.PAYE ? 'désactiver' : 'activer'} ce préfinancement ?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${pref.status ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: pref.status ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleStatus(pref);
      }
    });
  }

  /** Delete a prefinancement */
  deletePrefinancement(pref: PrefinancementResponseDTO) {
    this.confirmationService.confirm({
      message: `Supprimer le préfinancement ${pref.montant} MAD ?`,
      accept: () => {
        this.prefinancementService.deletePrefinancement(pref.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Préfinancement supprimé.' });
            this.loadPrefinancements(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer le préfinancement.' });
          }
        });
      }
    });
  }

  protected readonly Status = Status;
}
