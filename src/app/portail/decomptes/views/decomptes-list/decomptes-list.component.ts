import {Component, OnInit, ViewChild} from '@angular/core';
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {ButtonDirective} from "primeng/button";
import {Ripple} from "primeng/ripple";
import {DatePipe, DecimalPipe, NgClass, NgFor, NgForOf, NgIf} from "@angular/common";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {DropdownModule} from "primeng/dropdown";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ToastModule} from "primeng/toast";
import {FormsModule} from "@angular/forms";
import {DroitConstateResponseDto} from "../../../droitsConstate/dtos/DroitConstateResponseDto";
import {DroitConstateSearchDto} from "../../../droitsConstate/dtos/DroitConstateSearchDto";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {MarcheResponseDTO} from "../../../Marches/dtos/marche-response.dto";
import {Status} from "../../../prefinancements/enums/Status";
import {Type} from "../../../prefinancements/enums/Type";
import {DroitConstateService} from "../../../droitsConstate/service/DroitConstateService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MarcheService} from "../../../Marches/service/marche-service.service";
import {PageResponse} from "../../../global/types/PageResponse";
import {DecompteResponseDto} from "../../dtos/DecompteResponseDto";
import {DecompteService} from "../../service/DecompteService";
import {InputSwitchModule} from "primeng/inputswitch";

@Component({
  selector: 'app-decomptes-list',
  standalone: true,
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
    InputSwitchModule,
  ],
  templateUrl: './decomptes-list.component.html',
  styleUrl: './decomptes-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class DecomptesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  decomptes: DecompteResponseDto[] = [];
  totalRecords = 0;
  loading = true;

  // Filters
  searchCriteria: DroitConstateSearchDto = {};
  selectedBanque?: BanqueResponse;
  selectedMarche?: MarcheResponseDTO;
  selectedStatus?: Status;

  // Dropdown data sources
  banques: BanqueResponse[] = [];
  banquesTotal = 0;
  banquesLoading = false;

  marches: MarcheResponseDTO[] = [];
  marchesTotal = 0;
  marchesLoading = false;

  statusOptions: { label: string; value: Status | null }[] = [];

  // breadcrumb items
  items: MenuItem[] = [];

  constructor(
    private decompteService: DecompteService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/decomptes', label: 'Les Décomptes'},
    ];
    this.loadStatuses();
    this.loadDecomptes({first: 0, rows: 10});
  }

  /** Load décomptes with filters */
  loadDecomptes(event: TableLazyLoadEvent) {
    this.loading = true;

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.searchCriteria.banqueId = this.selectedBanque?.id;
    this.searchCriteria.marcheId = this.selectedMarche?.id;
    this.searchCriteria.status = this.selectedStatus;


    this.decompteService.getAllDecomptes(this.searchCriteria, page, size, sort)
      .subscribe({
        next: (res: PageResponse<DecompteResponseDto>) => {
          this.decomptes = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les décomptes.'
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
    this.applyFilters();
  }

  /** Mark a droit constaté as paid */
  toggleStatus(decompte: DecompteResponseDto) {

    this.decompteService.toggleStatus(decompte.id).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.'});
        this.loadDecomptes(this.table.createLazyLoadMetadata());
      },
      error: () => {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.'});
      }
    });
  }

  /** Delete a décompte */
  deleteDecompte(decompte: DecompteResponseDto) {
    this.confirmationService.confirm({
      message: `Supprimer la décompte d'un montant de ${decompte.montant} MAD ?`,
      accept: () => {
        this.decompteService.deleteDecompte(decompte.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'décompte  supprimé.'});
            this.loadDecomptes(this.table.createLazyLoadMetadata());
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Impossible de supprimer la décompte.'
            });
          }
        });
      }
    });
  }

  protected readonly Status = Status;
}

