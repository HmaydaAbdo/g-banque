import {Component, OnInit, ViewChild} from '@angular/core';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from 'primeng/api';
import {MarcheService} from '../../../Marches/service/marche-service.service';
import {BanqueService} from '../../../comptes-bancaire/services/BanqueService';
import {PageResponse} from '../../../global/types/PageResponse';
import {BanqueResponse} from '../../../comptes-bancaire/dtos/BanqueResponse';
import {MarcheResponseDTO} from '../../../Marches/dtos/marche-response.dto';
import {NgClass, NgFor, NgForOf, NgIf, DatePipe, DecimalPipe} from "@angular/common";
import {ButtonDirective} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {DropdownModule} from "primeng/dropdown";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ToastModule} from "primeng/toast";
import {FormsModule} from "@angular/forms";
import {DroitConstateResponseDto} from "../../dtos/DroitConstateResponseDto";
import {DroitConstateSearchDto} from "../../dtos/DroitConstateSearchDto";
import {Status} from "../../../prefinancements/enums/Status";
import {Type} from "../../../prefinancements/enums/Type";
import {DroitConstateService} from "../../service/DroitConstateService";
import {AuthService} from "../../../../auth/services/auth.service";
import {TagModule} from "primeng/tag";
import {InputSwitchModule} from "primeng/inputswitch";

@Component({
  selector: 'app-droit-constate-list',
  standalone: true,
  templateUrl: './droit-constate-list.component.html',
  styleUrls: ['./droit-constate-list.component.scss'],
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
export class DroitConstateListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  droitsConstates: DroitConstateResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: DroitConstateSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedMarche?: MarcheResponseDTO;
  selectedStatus?: Status;
  selectedType?: Type;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;

  marches: MarcheResponseDTO[] = [];
  marchesTotal = 0;
  marchesLoading = false;

  statusOptions: { label: string; value: Status | null }[] = [];
  typeOptions: { label: string; value: Type | null }[] = [];

  // breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private droitConstateService: DroitConstateService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    public authService:AuthService,
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/droits-constate', label: 'Droits constatés'},
    ];
    this.loadStatuses();
    this.loadTypes();
    this.loadDroitsConstates({first: 0, rows: 10});
  }

  /** Load droits constatés with filters */
  loadDroitsConstates(event: TableLazyLoadEvent) {
    this.loading = true;

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.marcheId = this.selectedMarche?.id;
    this.searchCriteria.status = this.selectedStatus;
    this.searchCriteria.type = this.selectedType;

    this.droitConstateService.getAllDroitConstates(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<DroitConstateResponseDto>) => {
          this.droitsConstates = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les droits constatés.'
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
      {label: 'Tous les types', value: null},
      {label: 'REMBOURSEMENT', value: Type.REMBOURSEMENT},
      {label: 'DEBLOCAGE', value: Type.DEBLOCAGE}
    ];
  }

  /** Load static status options */
  loadStatuses() {
    this.statusOptions = [
      {label: 'Tous les statuts', value: null},
      {label: 'Prévu', value: Status.PREVU},
      {label: 'Payé', value: Status.PAYE}
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
    this.selectedStatus = undefined;
    this.selectedType = undefined;
    this.applyFilters();
  }


  confirmToggleStatus(dc: DroitConstateResponseDto){
    const confirmationMessage = `Êtes-vous sûr de vouloir ${dc.status==Status.PAYE ? 'désactiver' : 'activer'} ce droit constaté ?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${dc.status ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: dc.status ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleStatus(dc);
      }
    });
  }

  /** Mark a droit constaté as paid */
  toggleStatus(dc: DroitConstateResponseDto) {
    if (dc.status === Status.PAYE) return;
    this.droitConstateService.toggleStatus(dc.id).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.'});
        this.loadDroitsConstates(this.table.createLazyLoadMetadata());
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.'});
      }
    });
  }


  /** Delete a droit constaté */
  deleteDroitConstate(dc: DroitConstateResponseDto) {
    this.confirmationService.confirm({
      message: `Supprimer le droit constaté d'un montant de ${dc.montant} MAD ?`,
      accept: () => {
        this.droitConstateService.deleteDroitConstate(dc.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Droit constaté supprimé.'});
            this.loadDroitsConstates(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Impossible de supprimer le droit constaté.'
            });
          }
        });
      }
    });
  }

  protected readonly Status = Status;
  protected readonly Type = Type;
}
