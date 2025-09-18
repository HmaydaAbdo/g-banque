import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import {ActivatedRoute, RouterLink, RouterLinkActive} from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms'; // Add FormsModule
import { BanqueResponse } from '../../dtos/BanqueResponse';
import { BanqueService } from '../../services/BanqueService';
import { LigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/LigneFonctionnelleService';
import { LigneFonctionnelleResponse } from '../../../lignes-fonctionnelles/dtos/LigneFonctionnelleResponse';
import { LigneFonctionnelleRequest } from '../../../lignes-fonctionnelles/dtos/LigneFonctionnelleRequest'; // Needed for update
import { LigneFonctionnelleSearchCriteria } from '../../../lignes-fonctionnelles/dtos/LigneFonctionnelleSearchCriteria';
import { NatureLigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service';
import { AuthService } from "../../../../auth/services/auth.service"; // Import AuthService
import { Role } from "../../../../auth/enum/Role"; // Import Role enum
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api'; // Import ConfirmationService
import { switchMap } from 'rxjs/operators'; // Import switchMap for chained observables


// PrimeNG Modules
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { PanelModule } from 'primeng/panel';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { InputSwitchModule } from 'primeng/inputswitch'; // Import InputSwitchModule
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PageResponse } from "../../../global/types/PageResponse";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BreadcrumbModule} from "primeng/breadcrumb";


@Component({
  selector: 'app-bank-history',
  standalone: true,
  imports: [
    CommonModule,
    TagModule,
    CardModule,
    PanelModule,
    TableModule,
    ButtonModule,
    RippleModule,
    DividerModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    FormsModule, // Add FormsModule here
    DropdownModule,
    InputNumberModule,
    TooltipModule,
    ToastModule,
    ConfirmDialogModule, // Add ConfirmDialogModule here
    InputSwitchModule,
    RouterLink,
    BreadcrumbModule,
    RouterLinkActive,
    // Add InputSwitchModule here
  ],
  providers: [MessageService, ConfirmationService], // Add ConfirmationService to providers
  templateUrl: './bank-history.component.html',
  styleUrls: ['./bank-history.component.scss']
})
export class BankHistoryComponent implements OnInit {
  @ViewChild('dtFunctionalLines') dtFunctionalLines: Table | undefined;

  bankId!: number;
  bankDetails: BanqueResponse | undefined;
  bankFunctionalLines: LigneFonctionnelleResponse[] = [];
  loading: boolean = true;
  loadingBankDetails: boolean = true;

  totalRecords: number = 0;
  rows: number = 3;
  first: number = 0;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;

  functionalLineSearchForm!: FormGroup;
  natureLigneOptions: { label: string; value: number | null }[] = [];

  // NEW: Options for the 'actif' dropdown filter
  actifOptions: any[] = [
    { label: 'Toutes', value: null },
    { label: 'Encours', value: true },
    { label: 'Archivées', value: false }
  ];

  items: MenuItem[] | undefined;

  constructor(
    private route: ActivatedRoute,
    private banqueService: BanqueService,
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService, // Inject ConfirmationService
    private authService: AuthService // Inject AuthService
  ) { }

  ngOnInit(): void {
    this.initFunctionalLineSearchForm();
    this.setupFunctionalLineFormListeners();
    this.loadNatureLigneOptions(); // This will now fetch from backend
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label:"Comptes bancaires" },
      { icon: 'pi pi-history', route: '/portail/select-for-history', label:"Détails & Historique" }
    ]

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bankId = +id;
        this.loadBankDetails();
        // loadBankFunctionalLines will be triggered by p-table's onLazyLoad
      }
    });
  }

  initFunctionalLineSearchForm(): void {
    this.functionalLineSearchForm = this.fb.group({
      natureLigneFonctionnelleId: [null],
      yearDepart: [null],
      actif: [true] // NEW: Default to showing only active functional lignePrefi
    });
  }

  setupFunctionalLineFormListeners(): void {
    this.functionalLineSearchForm.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onFunctionalLineSearch();
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

  loadBankDetails(): void {
    this.loadingBankDetails = true;
    this.banqueService.getBanqueById(this.bankId).subscribe({
      next: (data: BanqueResponse) => {
        this.bankDetails = data;
        this.loadingBankDetails = false;
      },
      error: (error) => {
        console.error('Error loading bank details:', error);
        this.loadingBankDetails = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des détails de la banque.', life: 3000 });
      }
    });
  }

  loadBankFunctionalLines(event: TableLazyLoadEvent): void {
    if (!this.bankId) {
      console.warn('Bank ID is not available to load functional lignePrefi.');
      this.loading = false;
      return;
    }

    this.loading = true;

    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    this.first = first;
    this.rows = rows as number;
    this.sortField = sortField;
    this.sortOrder = sortOrder;

    const page = (first as number) / (rows as number);

    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    // Get search criteria from the form, including the 'actif' status
    const searchCriteria: LigneFonctionnelleSearchCriteria = {
      ...this.functionalLineSearchForm.value,
    };

    this.ligneFonctionnelleService.getLignesFonctionnellesByBanqueId(this.bankId, searchCriteria, page, this.rows, sort).subscribe({
      next: (pageResponse: PageResponse<LigneFonctionnelleResponse>) => {
        this.bankFunctionalLines = pageResponse.content;
        this.totalRecords = pageResponse.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading bank functional lignePrefi:', error);
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des lignes fonctionnelles de la banque.', life: 3000 });
      }
    });
  }

  deleteLigneFonctionnelle(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la ligne fonctionnelle de nature "${ligne.natureLigneFonctionnelleLabel}" (ID: ${ligne.id}) pour la banque "${ligne.banqueNom}" ? Cette action est irréversible.`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        if (ligne.id) {
          this.ligneFonctionnelleService.deleteLigneFonctionnelle(ligne.id).subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne fonctionnelle supprimée avec succès.', life: 3000 });
              this.reloadBankFunctionalLinesTable(); // Reload data after successful deletion
            },
            error: (error) => {
              console.error('Erreur lors de la suppression de la ligne fonctionnelle:', error);
              const errorMessage = error.error?.message || 'Échec de la suppression de la ligne fonctionnelle.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            }
          });
        } else {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'ID de ligne fonctionnelle manquant pour la suppression.', life: 3000 });
        }
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Suppression annulée.', life: 3000 });
      }
    });
  }
  onFunctionalLineSearch(): void {
    this.first = 0; // Reset to the first page on new search
    if (this.dtFunctionalLines) {
      this.dtFunctionalLines.first = 0;
      this.dtFunctionalLines.onLazyLoad.emit(this.dtFunctionalLines.createLazyLoadMetadata());
    } else {
      this.loadBankFunctionalLines({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }



  //method to handle toggling active status
  onToggleActif(ligne: LigneFonctionnelleResponse, event: any): void {
    const newActifStatus = event.checked; // The new state of the switch
    const action = newActifStatus ? 'activer' : 'désactiver';

    this.confirmationService.confirm({
      message: `Voulez-vous vraiment ${action} la ligne fonctionnelle pour la banque "${ligne.banqueNom}" et la nature "${ligne.natureLigneFonctionnelleLabel}" ?`,
      header: `Confirmation d'${action}`,
      icon: 'pi pi-info-circle',
      accept: () => {

          if (!newActifStatus) { // If going from active to inactive (Deactivating)
            this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.id)
              .subscribe({
                next: () => {
                  this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Ligne fonctionnelle ${action}ée avec succès.`, life: 3000 });
                  this.reloadBankFunctionalLinesTable(); // Reload to reflect status change and re-apply filters
                },
                error: (error) => {
                  ligne.actif = !newActifStatus; // Revert UI switch on error
                  console.error(`Erreur lors de la ${action}ation de la ligne fonctionnelle:`, error);
                  const errorMessage = error.error?.message || `Échec de la ${action}ation de la ligne fonctionnelle.`;
                  this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
                }
              });
          } else { // If going from inactive to active (Activating)
            // For activation, we need to use the general update endpoint
            // First, get the full line to ensure all data is present for the PUT request
            this.ligneFonctionnelleService.getLigneFonctionnelleById(ligne.id!).pipe(
              switchMap(existingLigne => {
                const updateRequest: LigneFonctionnelleRequest = {
                  banqueId: existingLigne.banqueId,
                  natureLigneFonctionnelleId: existingLigne.natureLigneFonctionnelleId,
                  plafond: existingLigne.plafond,
                  taux: existingLigne.taux,
                  dateDepart: existingLigne.dateDepart,
                  dateFin: existingLigne.dateFin,
                  actif: newActifStatus // Set the new active status
                };
                return this.ligneFonctionnelleService.updateLigneFonctionnelle(ligne.id!, updateRequest);
              })
            ).subscribe({
              next: () => {
                this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Ligne fonctionnelle ${action}ée avec succès.`, life: 3000 });
                this.reloadBankFunctionalLinesTable(); // Reload to reflect status change and re-apply filters
              },
              error: (error) => {
                ligne.actif = !newActifStatus; // Revert UI switch on error
                console.error(`Erreur lors de l'${action}ation de la ligne fonctionnelle:`, error);
                const errorMessage = error.error?.message || `Échec de l'${action}ation de la ligne fonctionnelle.`;
                this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              }
            });
          }

      },
      reject: () => {
        ligne.actif = !newActifStatus; // Revert UI switch if user cancels
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: `Action d'${action}ation annulée.`, life: 3000 });
      }
    });
  }



  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }



  // Helper method to reload the functional lignePrefi table
  private reloadBankFunctionalLinesTable(): void {
    if (this.dtFunctionalLines) {
      this.dtFunctionalLines.first = 0; // Reset to first page
      this.dtFunctionalLines.onLazyLoad.emit(this.dtFunctionalLines.createLazyLoadMetadata()); // Trigger lazy load
    } else {
      // Fallback if table ref is not available
      this.loadBankFunctionalLines({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }
}
