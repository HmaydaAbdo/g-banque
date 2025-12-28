import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService, ConfirmationService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { InputSwitchModule } from 'primeng/inputswitch';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { LigneFonctionnelleSpecifiqueResponse } from "../../dtos/LigneFonctionnelleSpecifiqueResponse";
import { LigneFonctionnelleSpecifiqueSearchCriteria } from "../../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import { SpecificFunctionalLigneService } from "../../service/SpecificFunctionalLigneService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { MarcheService } from "../../../Marches/service/marche-service.service";
import { NatureLigneFonctionnelleResponse } from "../../../lignes-fonctionnelles/dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse";
import { NatureLigneFonctionnelleService } from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";

@Component({
  selector: 'app-spefic-ligne-archivees',
  templateUrl: './spefic-ligne-archivees.component.html',
  styleUrls: ['./spefic-ligne-archivees.component.scss'],
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
    CalendarModule,
    InputSwitchModule,
    BreadcrumbModule,
    RouterLinkActive
  ],
  providers: [MessageService, ConfirmationService]
})
export class SpeficLigneArchiveesComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  lignesFonctionnellesSpecifiques: LigneFonctionnelleSpecifiqueResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number = 10;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;

  banquesOptions: { label: string; value: number | null }[] = [];
  natureLigneOptions: { label: string; value: number | null }[] = [];
  items: MenuItem[] | undefined;

  // ✅ Configuration optimisée pour le dropdown des marchés
  marchesOptions: any[] = [];
  marchesLoading: boolean = false;
  currentMarcheFilter: string = '';
  rowsPerPageMarches: number = 10;

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  constructor(
    private specificLigneService: SpecificFunctionalLigneService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-specifiques-archived', label: "Les Lignes Fonctionnelles Spécifiques Archivées" }
    ];
    this.initSearchForm();
    this.setupFormListeners();
    this.loadFilterOptions();
    this.initializeMarches(); // ✅ Initialisation des marchés
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
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

  loadFilterOptions(): void {
    this.loadBanques();
    this.loadNatureLigneOptions();
  }

  loadNatureLigneOptions(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures: NatureLigneFonctionnelleResponse[]) => {
        this.natureLigneOptions = [{ label: 'Toutes les natures', value: null }];
        natures.forEach(nature => {
          this.natureLigneOptions.push({ label: nature.label, value: nature.id! });
        });
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des natures de ligne pour le filtre.',
          life: 3000
        });
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
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des banques pour le filtre.',
            life: 3000
          });
          console.error('Error loading banks for filter:', err);
        }
      });
  }

  // ✅ Initialisation des marchés avec placeholders
  initializeMarches(): void {
    this.marchesLoading = true;
    this.marcheService.getAllMarchesForLazyLoad(0, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        // ✅ Option "Tous les marchés" en première position
        const allOption = { label: 'Tous les marchés', value: null };

        // ✅ Crée un tableau avec l'option "Tous" + placeholders vides
        this.marchesOptions = [allOption, ...Array.from({ length: total }).map(() => ({}))];

        // ✅ Remplit les premiers éléments réels (en sautant l'index 0 qui contient "Tous")
        this.marchesOptions.splice(1, data.length, ...data);
        this.marchesLoading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec de l\'initialisation des marchés.',
          life: 3000
        });
        console.error('Error initializing markets:', err);
        this.marchesLoading = false;
      }
    });
  }

  // ✅ Chargement lazy avec cache
  onLazyLoadMarches(event: any): void {
    const { first } = event;

    // ✅ Si c'est l'option "Tous les marchés" (index 0), on skip
    if (first === 0) return;

    const page = Math.floor((first - 1) / this.rowsPerPageMarches); // -1 car index 0 = "Tous"

    // ✅ Vérifie si les données sont déjà chargées (cache)
    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) {
      return; // Pas besoin de recharger
    }

    this.marchesLoading = true;
    this.marcheService.getAllMarches(
      { numero: this.currentMarcheFilter },
      page,
      this.rowsPerPageMarches,
      'numero,asc'
    ).subscribe({
      next: (res) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        const updatedOptions = [...this.marchesOptions];

        // ✅ Remplace les placeholders par les vraies données
        updatedOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = updatedOptions;
        this.marchesLoading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des marchés.',
          life: 3000
        });
        console.error('Error loading markets:', err);
        this.marchesLoading = false;
      }
    });
  }

  // ✅ Filtrage serveur
  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentMarcheFilter = query;

    this.marchesLoading = true;
    this.marcheService.getAllMarches(
      { numero: query },
      0,
      this.rowsPerPageMarches,
      'numero,asc'
    ).subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        // ✅ Option "Tous les marchés" + placeholders
        const allOption = { label: 'Tous les marchés', value: null };
        this.marchesOptions = [allOption, ...Array.from({ length: total }).map(() => ({}))];
        this.marchesOptions.splice(1, data.length, ...data);
        this.marchesLoading = false;
      },
      error: (err) => {
        console.error('Error filtering markets:', err);
        this.marchesLoading = false;
      }
    });
  }

  loadLignesFonctionnellesSpecifiques(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.first = event.first !== undefined ? event.first : 0;
    this.rows = (event.rows !== undefined ? event.rows : 10) as number;
    this.sortField = event.sortField || 'id';
    this.sortOrder = event.sortOrder === 1 ? 1 : -1;
    const page = this.first / this.rows;
    const actualSortField = Array.isArray(this.sortField) ? this.sortField[0] : this.sortField;
    const sort = `${actualSortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`;
    const searchCriteria: LigneFonctionnelleSpecifiqueSearchCriteria = this.searchForm.value;

    this.specificLigneService
      .getLignesFonctionnellesSpecifiques(searchCriteria, page, this.rows, sort)
      .subscribe({
        next: (data) => {
          this.lignesFonctionnellesSpecifiques = data.content;
          this.totalRecords = data.page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Échec du chargement des lignes fonctionnelles spécifiques.',
            life: 3000
          });
          console.error('Error fetching specific functional lignePrefi:', err);
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.dt.filterTimeout = 0;
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      this.loadLignesFonctionnellesSpecifiques({
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
      marcheId: null,
      actif: false,
    });
  }
}
