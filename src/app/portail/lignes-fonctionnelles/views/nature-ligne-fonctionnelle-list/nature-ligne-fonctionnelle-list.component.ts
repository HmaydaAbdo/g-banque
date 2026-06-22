import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog'; // Keep if you use it elsewhere
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import {MessageService, ConfirmationService, MenuItem} from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { NatureLigneFonctionnelleService } from '../../services/nature-ligne-fonctionnelle.service';
import { NatureLigneFonctionnelleResponse } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse';
import { NatureLigneFonctionnelleSearchCriteria } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleSearchCriteria';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms'; // Import reactive forms modules
import { RippleModule } from 'primeng/ripple';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators'; // Import RxJS operators
import { TooltipModule } from 'primeng/tooltip';
import {DropdownModule} from "primeng/dropdown";
import {AuthService} from "../../../../auth/services/auth.service";
import {BreadcrumbModule} from "primeng/breadcrumb";
import { PorteeNature, PORTEE_NATURE_LABELS } from '../../dtos/nature-ligne-fonctionnelle/PorteeNature';
import {TagModule} from "primeng/tag";

@Component({
  selector: 'app-nature-ligne-fonctionnelle-list',
  templateUrl: './nature-ligne-fonctionnelle-list.component.html',
  styleUrls: ['./nature-ligne-fonctionnelle-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DialogModule,
    ConfirmDialogModule,
    ToastModule,
    ReactiveFormsModule,
    RippleModule,
    TooltipModule,
    DropdownModule,
    BreadcrumbModule,
    RouterLink,
    RouterLinkActive,
    TagModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class NatureLigneFonctionnelleListComponent implements OnInit {
  @ViewChild('dt') dt: Table | undefined; // Get a reference to the p-table

  natures: NatureLigneFonctionnelleResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  /** Options du dropdown Portée pour le filtre de recherche */
  porteeFilterOptions = [
    { label: 'Toutes les portées', value: null },
    ...Object.values(PorteeNature).map(value => ({
      label: PORTEE_NATURE_LABELS[value],
      value
    }))
  ];

  /** Expose les labels de portée au template */
  porteeLabel = PORTEE_NATURE_LABELS;

  constructor(
    private natureService: NatureLigneFonctionnelleService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label:"Les Lignes fonctionnelles" },
      { icon: 'pi pi-list', route: '/portail/natures-fonctionnelles', label:"Les Natures des Lignes fonctionnelles" },
    ]
    this.initSearchForm();
    this.setupFormListeners();

  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      code: [null],
      label: [null],
      description: [null],
      portee: [null]
    });
  }

  setupFormListeners(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)) // Only emit if value actually changed
      )
      .subscribe(() => {
        this.onSearch();
      });
  }

  loadNatures(event: TableLazyLoadEvent = { first: 0, rows: 10 }): void {
    this.loading = true;

    this.first = event.first !== undefined ? event.first : 0;
    const page = this.first / ( (event.rows !== undefined ? event.rows : 10) as number );
    const size = (event.rows !== undefined ? event.rows : 10) as number;
    const sortField = event.sortField || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    // Get search criteria directly from the form
    const searchCriteria: NatureLigneFonctionnelleSearchCriteria = this.searchForm.value;

    this.natureService.getAllNatureLigneFonctionnelles(searchCriteria, page, size, sort)
      .subscribe({
        next: (pageResponse) => {
          this.natures = pageResponse.content;
          this.totalRecords = pageResponse.page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          console.error('Erreur lors du chargement des natures:', err);
          this.loading = false;
        }
      });
  }

  onSearch(): void {
    if (this.dt) {
      // Reset to first page when new search criteria are applied
      this.dt.first = 0;
      // This will trigger loadNatures via onLazyLoad
      this.dt.filterTimeout = 0; // Ensure immediate filter application
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      // Fallback if dt is not available (shouldn't happen with @ViewChild)
      this.loadNatures({
        first: 0,
        rows: 10,
        sortField: 'id',
        sortOrder: 1,
        filters: {}
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      code: null,
      label: null,
      description: null,
      portee: null
    });
    // The valueChanges subscription will automatically trigger a reload via onSearch()
  }

  editNature(id: number): void {
    this.router.navigate([`/portail/natures-fonctionnelles/edit/${id}`]);
  }

  deleteNature(id: number): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette nature de ligne fonctionnelle ? Cette action est irréversible.',
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.natureService.deleteNatureLigneFonctionnelle(id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Nature de ligne fonctionnelle supprimée avec succès.', life: 3000 });
            // Reload the list, similar to LignesFonctionnellesComponent
            if (this.dt) {
              this.dt.first = 0; // Go back to first page after deletion
              this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata()); // Reload the list
            } else {
              this.loadNatures();
            }
          },
          error: (error) => {
            console.error('Erreur lors de la suppression de la nature de ligne fonctionnelle:', error);
          }
        });
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Suppression annulée.', life: 3000 });
      }
    });
  }

  addNewNature(): void {
    this.router.navigate(['/portail/natures-fonctionnelles/new']);
  }

  isAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  getPorteeLabel(portee: any): string {
    // Use a type cast or fallback to an empty string/label if not found
    return this.porteeLabel[portee as PorteeNature] || 'N/A';
  }
}
