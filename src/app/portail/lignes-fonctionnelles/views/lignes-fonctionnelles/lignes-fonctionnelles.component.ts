import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Application-specific imports
import { LigneFonctionnelleResponse } from "../../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleSearchCriteria } from "../../dtos/LigneFonctionnelleSearchCriteria";
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from "../../../../auth/services/auth.service";
import { Role } from "../../../../auth/enum/Role";
import {NatureLigneFonctionnelleService} from "../../services/nature-ligne-fonctionnelle.service";


@Component({
  selector: 'app-lignes-fonctionnelles',
  templateUrl: './lignes-fonctionnelles.component.html',
  styleUrls: ['./lignes-fonctionnelles.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DecimalPipe,
    DatePipe,
    RouterLink,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    ToolbarModule,
    TagModule,
    TooltipModule,
    DropdownModule,
    InputNumberModule,
    CalendarModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class LignesFonctionnellesComponent implements OnInit {

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

  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService, // NEW: Inject NatureLigneFonctionnelleService
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
  ) { }

  ngOnInit(): void {
    this.initSearchForm();
    this.setupFormListeners();
    this.loadBanques();
    this.loadNatureLigneOptions(); // This will now fetch from backend
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
    });
  }

  setupFormListeners(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        // When form values change, trigger a new search
        this.onSearch();
      });
  }


  loadNatureLigneOptions(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {
        this.natureLigneOptions = [{ label: 'Toutes les natures', value: null }];
        natures.forEach(nature => {
          this.natureLigneOptions.push({ label: nature.label, value: nature.id! }); // Use description for display, ID for value
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

    // Pass the form values directly as search criteria
    const searchCriteria: LigneFonctionnelleSearchCriteria = this.searchForm.value;

    this.ligneFonctionnelleService
      .getCurrentLigneFonctionnelles(searchCriteria, page, this.rows, sort)
      .subscribe({
        next: (data) => {
          this.lignesFonctionnelles = data.content;
          this.totalRecords = data.totalElements;
          this.loading = false;

        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des lignes fonctionnelles.', life: 3000 });
          console.error('Error fetching functional lines:', err);
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    if (this.dt) {
      // Reset to first page when new search criteria are applied
      this.dt.first = 0;
      // This will trigger loadLignesFonctionnelles via onLazyLoad
      this.dt.filterTimeout = 0; // Ensure immediate filter application
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      this.loadLignesFonctionnelles({
        first: 0,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {} // No specific filters needed here, form valueChanges handles it
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      banqueId: null,
      natureLigneFonctionnelleId: null,
    });
    // The valueChanges subscription will automatically trigger a reload
  }


  deleteLigneFonctionnelle(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      // Changed: Use natureLigneFonctionnelleLabel from response
      message: `Êtes-vous sûr de vouloir supprimer la ligne fonctionnelle avec la nature "${ligne.natureLigneFonctionnelleLabel}" pour la banque "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.ligneFonctionnelleService.deleteLigneFonctionnelle(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle supprimée avec succès.', life: 3000 });
              if (this.dt) {
                this.dt.first = 0; // Go back to first page after deletion
                this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata()); // Reload the list
              } else {
                this.loadLignesFonctionnelles({ first: this.first, rows: this.rows, sortField: this.sortField, sortOrder: this.sortOrder, filters: {} });
              }
            },
            error: (error) => {
              console.error('Erreur lors de la suppression de la ligne fonctionnelle:', error);
              const errorMessage = error.error?.message || 'Échec de la suppression de la ligne fonctionnelle.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }



  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }
}
