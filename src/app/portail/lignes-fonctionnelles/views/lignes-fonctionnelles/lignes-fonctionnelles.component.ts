import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
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
import { InputSwitchModule } from 'primeng/inputswitch';
import { switchMap } from 'rxjs/operators'; //

// Application-specific imports
import { LigneFonctionnelleResponse } from "../../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleRequest } from "../../dtos/LigneFonctionnelleRequest"; // Ensure this is imported if needed for updates
import { LigneFonctionnelleSearchCriteria } from "../../dtos/LigneFonctionnelleSearchCriteria";
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from "../../../../auth/services/auth.service";
import { Role } from "../../../../auth/enum/Role";
import { NatureLigneFonctionnelleService } from "../../services/nature-ligne-fonctionnelle.service";


@Component({
  selector: 'app-lignes-fonctionnelles',
  templateUrl: './lignes-fonctionnelles.component.html',
  styleUrls: ['./lignes-fonctionnelles.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
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
    CalendarModule,
    InputSwitchModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class LignesFonctionnellesComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  lignesFonctionnelles: LigneFonctionnelleResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number = 3;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;
  natureLigneOptions: { label: string; value: number | null }[] = [];
  banquesOptions: { label: string; value: number | null }[] = [];

  // NEW: Options for the 'actif' dropdown filter
  actifOptions: any[] = [
    { label: 'Toutes', value: null },
    { label: 'Actives', value: true },
    { label: 'Inactives', value: false }
  ];

  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
  ) { }

  ngOnInit(): void {
    this.initSearchForm();
    this.setupFormListeners();
    this.loadBanques();
    this.loadNatureLigneOptions();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      actif: [true] // Default to showing only active ones
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

    // Get search criteria directly from the form, including the 'actif' status
    const searchCriteria: LigneFonctionnelleSearchCriteria = {
      ...this.searchForm.value,
      // If the user can't manage active status, ensure 'actif' is always true for the request
      actif: this.canManageActifStatus() ? this.searchForm.get('actif')?.value : true
    };

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
      actif: this.canManageActifStatus() ? null : true // Reset actif to null for admins, true for others
    });
    // The valueChanges subscription will automatically trigger a reload
  }

  deleteLigneFonctionnelle(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la ligne fonctionnelle avec la nature "${ligne.natureLigneFonctionnelleLabel}" pour la banque "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.ligneFonctionnelleService.deleteLigneFonctionnelle(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle supprimée avec succès.', life: 3000 });
              this.reloadTableAfterAction();
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

  // NEW: Method to handle toggling active status
  onToggleActif(ligne: LigneFonctionnelleResponse, event: any): void {
    const newActifStatus = event.checked; // The new state of the switch
    const action = newActifStatus ? 'activer' : 'désactiver';

    this.confirmationService.confirm({
      message: `Voulez-vous vraiment ${action} la ligne fonctionnelle pour la banque "${ligne.banqueNom}" et la nature "${ligne.natureLigneFonctionnelleLabel}" ?`,
      header: `Confirmation d'${action}`,
      icon: 'pi pi-info-circle',
      accept: () => {
        if (ligne.banqueId && ligne.natureLigneFonctionnelleId) {
          // IMPORTANT: Call the specific deactivate endpoint if deactivating,
          // or a general update if reactivating.
          // For simplicity, we'll use the dedicated `deactivateLigneFonctionnelle`
          // for deactivation, and assume `updateLigneFonctionnelle` for activation.
          // Your backend logic will determine the exact API call.

          // Option 1: If backend has specific activate/deactivate endpoints
          if (!newActifStatus) { // If going from active to inactive
            this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.banqueId, ligne.natureLigneFonctionnelleId)
              .subscribe({
                next: () => {
                  this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Ligne fonctionnelle ${action}ée avec succès.`, life: 3000 });
                  // No need to manually update `ligne.actif` here; the reload will fetch fresh data.
                  this.reloadTableAfterAction();
                },
                error: (error) => {
                  // Revert the UI switch if the API call fails
                  ligne.actif = !newActifStatus;
                  console.error(`Erreur lors de la ${action}ation de la ligne fonctionnelle:`, error);
                  const errorMessage = error.error?.message || `Échec de la ${action}ation de la ligne fonctionnelle.`;
                  this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
                }
              });
          } else { // If going from inactive to active (via general update)
            // This assumes your backend's PUT /lignes-fonctionnelles/{id} can handle activation
            // You'll need to fetch the full existing line, update its 'actif' status, and then send.
            this.ligneFonctionnelleService.getLigneFonctionnelleById(ligne.id!).pipe(
              switchMap(existingLigne => {
                const updateRequest: LigneFonctionnelleRequest = {
                  banqueId: existingLigne.banqueId,
                  natureLigneFonctionnelleId: existingLigne.natureLigneFonctionnelleId,
                  plafond: existingLigne.plafond,
                  taux: existingLigne.taux,
                  dateDepart: existingLigne.dateDepart,
                  dateFin: existingLigne.dateFin,
                  actif: newActifStatus // Set the new status
                };
                return this.ligneFonctionnelleService.updateLigneFonctionnelle(ligne.id!, updateRequest);
              })
            ).subscribe({
              next: () => {
                this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Ligne fonctionnelle ${action}ée avec succès.`, life: 3000 });
                this.reloadTableAfterAction();
              },
              error: (error) => {
                ligne.actif = !newActifStatus;
                console.error(`Erreur lors de l'${action}ation de la ligne fonctionnelle:`, error);
                const errorMessage = error.error?.message || `Échec de l'${action}ation de la ligne fonctionnelle.`;
                this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              }
            });
          }
        }
      },
      reject: () => {
        // Revert the UI switch if the user cancels the confirmation
        ligne.actif = !newActifStatus;
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: `Action d'${action}ation annulée.`, life: 3000 });
      }
    });
  }

  // NEW: Method for explicit deactivate button
  deactivateLigneFonctionnelle(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      message: `Voulez-vous désactiver la ligne fonctionnelle pour la banque "${ligne.banqueNom}" et la nature "${ligne.natureLigneFonctionnelleLabel}" ?`,
      header: 'Confirmation de Désactivation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        if (ligne.banqueId && ligne.natureLigneFonctionnelleId) {
          this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.banqueId, ligne.natureLigneFonctionnelleId)
            .subscribe({
              next: () => {
                this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne fonctionnelle désactivée avec succès.', life: 3000 });
                this.reloadTableAfterAction();
              },
              error: (error) => {
                console.error('Erreur lors de la désactivation de la ligne fonctionnelle:', error);
                const errorMessage = error.error?.message || 'Échec de la désactivation de la ligne fonctionnelle.';
                this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              }
            });
        }
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Désactivation annulée.', life: 3000 });
      }
    });
  }


  // Check if the user is ADMIN
  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }

  // Check if the user is ADMIN or SUPERADMIN to manage active status
  canManageActifStatus(): boolean {
    return this.authService.hasRole(Role.ADMIN) || this.authService.hasRole(Role.SUPER_ADMIN);
  }

  // Helper to reload the table after an action (delete/toggle)
  private reloadTableAfterAction(): void {
    if (this.dt) {
      this.dt.first = 0; // Go back to first page
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata()); // Reload data
    } else {
      // Fallback in case dt is not ready, though with @ViewChild it usually is
      this.loadLignesFonctionnelles({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }
}
