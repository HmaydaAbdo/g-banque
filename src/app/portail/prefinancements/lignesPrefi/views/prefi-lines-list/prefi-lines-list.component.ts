import { Component, OnInit, ViewChild } from '@angular/core';
import { DatePipe, NgClass, NgForOf } from '@angular/common';
import { TableLazyLoadEvent, TableModule, Table } from 'primeng/table';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuItem, MessageService, PrimeTemplate } from 'primeng/api';
import { SpecificFunctionalLigneService } from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import { PageResponse } from "../../../../global/types/PageResponse";
import { ToastModule } from "primeng/toast";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DropdownModule } from "primeng/dropdown";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { Ripple } from "primeng/ripple";
import { SpecifiqueLineSearchDto } from "../../dtos/SpecifiqueLineSearchDto";
import { MarcheResponseDTO } from "../../../../Marches/dtos/marche-response.dto";
import { BanqueResponse } from "../../../../comptes-bancaire/dtos/BanqueResponse";
import { MarcheService } from "../../../../Marches/service/marche-service.service";
import { BanqueService } from "../../../../comptes-bancaire/services/BanqueService";
import { Subject } from 'rxjs'; // Import Subject
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {SpecificLigneResponseDto} from "../../../../specific-Functional-Lignes/dtos/SpecificLigneResponseDto"; // Import operators

@Component({
  selector: 'app-prefi-lines-list',
  standalone: true,
  imports: [
    TableModule,
    BreadcrumbModule,
    ButtonModule,
    DatePipe,
    NgForOf,
    ToastModule,
    ConfirmDialogModule,
    NgClass,
    DropdownModule,
    FormsModule,
    ReactiveFormsModule,
    Ripple,
    PrimeTemplate,
  ],
  templateUrl: './prefi-lines-list.component.html',
  styleUrls: ['./prefi-lines-list.component.scss']
})
export class PrefiLinesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  prefiLines: SpecificLigneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Dropdown data and search criteria
  selectedMarche?: MarcheResponseDTO;
  selectedBanque?: BanqueResponse;

  marches: MarcheResponseDTO[] = [];
  marchesLoading = false;

  banques: BanqueResponse[] = [];
  banquesLoading = false;

  // RxJS Subject for debouncing filter changes
  private filterSubject = new Subject<void>();

  constructor(
    private specifiqueFunctionalLigneService: SpecificFunctionalLigneService,
    private messageService: MessageService,
    private marcheService: MarcheService,
    private banqueService: BanqueService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', label: 'Accueil', routerLink: '/portail/accueil' },
      { icon: 'pi pi-list', label: 'Lignes de Préfinancement' }
    ];

    // Subscribe to the filter subject with a debounce time
    this.filterSubject.pipe(
      debounceTime(400), // Adjust debounce time as needed (e.g., 400ms)
      distinctUntilChanged()
    ).subscribe(() => {
      if (this.table) {
        this.table.reset();
      }
    });
  }

  /**
   * Handles the lazy load event for the PrimeNG table, fetching paginated, sorted, and filtered data.
   * @param event The lazy load event containing pagination and sorting details.
   */
  loadPrefiLines(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField as string || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: SpecifiqueLineSearchDto = {
      marcheId: this.selectedMarche?.id,
      banqueId: this.selectedBanque?.id
    };

    this.specifiqueFunctionalLigneService.getPrefi(searchCriteria, page, size, sort).subscribe({
      next: (res: PageResponse<SpecificLigneResponseDto>) => {
        this.prefiLines = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les lignes de préfinancement.', life: 3000 });
        console.error(err);
      }
    });
  }

  /**
   * Loads marches lazily for the dropdown.
   * @param event The lazy load event.
   */
  loadMarchesLazy(event: any): void {
    this.marchesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    this.marcheService.getAllMarches({}, page, size, 'numero,asc').subscribe({
      next: (res) => {
        this.marches = res.content;
        this.marchesLoading = false;
      },
      error: () => this.marchesLoading = false
    });
  }

  /**
   * Loads banques lazily for the dropdown.
   * @param event The lazy load event.
   */
  loadBanquesLazy(event: any): void {
    this.banquesLoading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    this.banqueService.getAllBanques({}, page, size, 'nom,asc').subscribe({
      next: (res) => {
        this.banques = res.content;
        this.banquesLoading = false;
      },
      error: () => this.banquesLoading = false
    });
  }

  /**
   * Emits a value to the filter subject, triggering a debounced table reset.
   */
  applyFilters(): void {
    this.filterSubject.next();
  }

  /**
   * Clears all filters and reloads the table.
   */
  clearFilters(): void {
    this.selectedMarche = undefined;
    this.selectedBanque = undefined;
    this.applyFilters();
  }
}
