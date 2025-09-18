import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule, DatePipe, DecimalPipe} from "@angular/common";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RouterLink, RouterLinkActive} from "@angular/router";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {InputTextModule} from "primeng/inputtext";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ToolbarModule} from "primeng/toolbar";
import {TagModule} from "primeng/tag";
import {TooltipModule} from "primeng/tooltip";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {CalendarModule} from "primeng/calendar";
import {InputSwitchModule} from "primeng/inputswitch";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ConfirmationService, MenuItem, MessageService} from "primeng/api";
import {LigneFonctionnelleResponse} from "../../dtos/LigneFonctionnelleResponse";
import {LigneFonctionnelleService} from "../../services/LigneFonctionnelleService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {NatureLigneFonctionnelleService} from "../../services/nature-ligne-fonctionnelle.service";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {LigneFonctionnelleSearchCriteria} from "../../dtos/LigneFonctionnelleSearchCriteria";


@Component({
  selector: 'app-lignes-fonctionnelles-expirees',
  standalone: true,
  templateUrl: './lignes-fonctionnelles-expirees.component.html',
  styleUrl: './lignes-fonctionnelles-expirees.component.scss',
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
    InputSwitchModule,
    BreadcrumbModule,
    RouterLinkActive
  ],
  providers: [MessageService, ConfirmationService]
})
export class LignesFonctionnellesExpireesComponent implements OnInit {

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
  items: MenuItem[] | undefined;

  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,

  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },{ icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-expired', label:"Les Lignes fonctionnelles Expirées" } ]
    this.initSearchForm();
    this.setupFormListeners();
    this.loadBanques();
    this.loadNatureLigneOptions();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      actif: [true],
      isExpired: [true],
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
      actif: true,
      isExpired: true,
    });
    // The valueChanges subscription will automatically trigger a reload
  }

  desactiver(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir désactiver la ligne fonctionnelle "${ligne.natureLigneFonctionnelleLabel}" avec  "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Désactivation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle désactivée avec succès.', life: 3000 });
              this.reloadTableAfterAction();
            },
            error: (error) => {
              console.error('Erreur lors de la désactivation de la ligne fonctionnelle:', error);
              const errorMessage = error.error?.message;
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
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
