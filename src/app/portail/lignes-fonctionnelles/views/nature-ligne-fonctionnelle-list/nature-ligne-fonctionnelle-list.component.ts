import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog'; // Keep if you use it elsewhere
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { NatureLigneFonctionnelleService } from '../../services/nature-ligne-fonctionnelle.service';
import { NatureLigneFonctionnelleResponse } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse';
import { NatureLigneFonctionnelleSearchCriteria } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleSearchCriteria';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms'; // Import reactive forms modules
import { RippleModule } from 'primeng/ripple';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators'; // Import RxJS operators
import { TooltipModule } from 'primeng/tooltip';
import {DropdownModule} from "primeng/dropdown";
import {Role} from "../../../../auth/enum/Role";
import {AuthService} from "../../../../auth/services/auth.service"; // Add tooltip module for consistency

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
    DatePipe,
    TooltipModule,
    DropdownModule
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

  constructor(
    private natureService: NatureLigneFonctionnelleService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.initSearchForm();
    this.setupFormListeners();

  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      label: [null],
      description: [null]
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
          this.totalRecords = pageResponse.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des natures de ligne.', life: 3000 });
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
      label: null,
      description: null
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
            const errorMessage = error.error?.message || 'Échec de la suppression de la nature de ligne fonctionnelle.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
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
    return this.authService.hasRole(Role.ADMIN);
  }
}
