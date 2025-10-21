import {Component, OnInit, ViewChild} from '@angular/core';
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ButtonDirective} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DatePipe, DecimalPipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {DropdownModule} from "primeng/dropdown";
import {PaginatorModule} from "primeng/paginator";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {Ripple} from "primeng/ripple";
import {RouterLinkActive} from "@angular/router";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {LigneFonctionnelleResponse} from "../../dtos/LigneFonctionnelleResponse";
import {LigneFonctionnelleService} from "../../services/LigneFonctionnelleService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {NatureLigneFonctionnelleService} from "../../services/nature-ligne-fonctionnelle.service";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {LigneFonctionnelleSearchCriteria} from "../../dtos/LigneFonctionnelleSearchCriteria";


@Component({
  selector: 'app-lignes-fonctionnelles-archives',
  standalone: true,
  imports: [
    BreadcrumbModule,
    ButtonDirective,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    NgForOf,
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass
  ],
  templateUrl: './lignes-fonctionnelles-archives.component.html',
  styleUrl: './lignes-fonctionnelles-archives.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class LignesFonctionnellesArchivesComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  lignesFonctionnelles: LigneFonctionnelleResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number = 10;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;
  natureLigneOptions: { label: string; value: number | null }[] = [];
  banquesOptions: { label: string; value: number | null }[] = [];
  items: MenuItem[] | undefined;
  expandedRows={}
  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },{ icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-archived', label:"Les Lignes fonctionnelles Archivées" } ]
    this.initSearchForm();
    this.setupFormListeners();
    this.loadBanques();
    this.loadNatureLigneOptions();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      actif: [false],
    });
  }

  setupFormListeners(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onSearch();
      });
  }

  loadNatureLigneOptions(): void {


    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {
        this.natureLigneOptions = [{ label: 'Toutes les natures', value: null }];
        natures.forEach(nature => {
          this.natureLigneOptions.push({ label: nature.label, value: nature.id! });
        });
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des natures de ligne pour le filtre.', life: 3000 });
        console.error('Error loading natures for filter:', err);
      }
    });
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .subscribe({
        next: (pageResponse) => {
          this.banquesOptions = [{ label: 'Toutes les banques', value: null }];
          pageResponse.content.forEach(banque => {
            this.banquesOptions.push({ label: banque.nom, value: banque.id! });
          });
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques pour le filtre.', life: 3000 });
          console.error('Error loading banks for filter:', err);
        }
      });
  }

  loadLignesFonctionnelles(event: TableLazyLoadEvent): void {
    this.loading = true;

    this.first = event.first !== undefined ? event.first : 0;
    this.rows = (event.rows !== undefined ? event.rows : 10) as number;
    this.sortField = event.sortField || 'id';
    this.sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = this.first / this.rows;

    const actualSortField = Array.isArray(this.sortField) ? this.sortField[0] : this.sortField;
    const sort = `${actualSortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`;

    // Get search criteria directly from the form
    const searchCriteria: LigneFonctionnelleSearchCriteria = this.searchForm.value;

    this.ligneFonctionnelleService
      .getCurrentLigneFonctionnelles(searchCriteria, page, this.rows, sort)
      .subscribe({
        next: (data) => {
          this.lignesFonctionnelles = data.content;
          this.totalRecords = data.page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des lignes fonctionnelles.', life: 3000 });
          console.error('Error fetching functional lignePrefi:', err);
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0; // Reset to first page when new search criteria are applied
      this.dt.filterTimeout = 0; // Ensure immediate filter application
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata()); // Trigger lazy load
    } else {
      this.loadLignesFonctionnelles({
        first: 0,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      banqueId: null,
      natureLigneFonctionnelleId: null,
      actif: false
    });
    // The valueChanges subscription will automatically trigger a reload
  }



}
