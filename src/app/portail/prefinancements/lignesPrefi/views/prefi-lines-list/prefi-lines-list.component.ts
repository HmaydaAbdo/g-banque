import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuItem, MessageService, PrimeTemplate, ScrollerOptions } from 'primeng/api';
import { ToastModule } from "primeng/toast";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DropdownModule } from "primeng/dropdown";
import { RippleModule } from "primeng/ripple";
import { TooltipModule } from 'primeng/tooltip';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Services & DTOs
import { SpecificFunctionalLigneService } from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import { MarcheService } from "../../../../Marches/service/marche-service.service";
import { BanqueService } from "../../../../comptes-bancaire/services/BanqueService";
import { SpecificLigneResponseDto } from "../../../../specific-Functional-Lignes/dtos/SpecificLigneResponseDto";
import { SpecifiqueLineSearchDto } from "../../dtos/SpecifiqueLineSearchDto";

@Component({
  selector: 'app-prefi-lines-list',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    BreadcrumbModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    DropdownModule,
    FormsModule,
    ReactiveFormsModule,
    RippleModule,
    TooltipModule,
    DecimalPipe,
    DatePipe,
    RouterLink
  ],
  providers: [MessageService],
  templateUrl: './prefi-lines-list.component.html',
  styleUrls: ['./prefi-lines-list.component.scss']
})
export class PrefiLinesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  prefiLines: SpecificLigneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Configuration Marchés (Lazy + Virtual Scroll)
  marchesOptions: any[] = [];
  marchesLoading = false;
  currentMarcheFilter = '';
  rowsPerPageMarches = 10;

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  banques: any[] = [];
  private filterTimeout: any;

  constructor(
    private fb: FormBuilder,
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

    this.initForm();
    this.initializeMarches();
    this.loadBanquesSimple();

    // Ecoute les changements du formulaire pour recharger la table avec debounce
    this.searchForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      if (this.table) {
        this.table.reset();
      }
    });
  }

  initForm(): void {
    this.searchForm = this.fb.group({
      marcheId: [null],
      banqueId: [null]
    });
  }

  // --- LOGIQUE DROPDOWN MARCHÉS (Filtrage Serveur + Virtual Scroll) ---

  initializeMarches(filter: string = ''): void {
    this.marchesLoading = true;
    // On passe le filtre numero à l'API
    this.marcheService.getAllMarches({ numero: filter }, 0, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        // Initialisation du tableau avec des placeholders pour le scroll virtuel
        this.marchesOptions = Array.from({ length: total }).map(() => ({}));
        this.marchesOptions.splice(0, data.length, ...data);
        this.marchesOptions = [...this.marchesOptions]; // Force la détection
        this.marchesLoading = false;
      },
      error: () => this.marchesLoading = false
    });
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    // Si la donnée est déjà présente à cet index, on ne recharge pas
    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) return;

    this.marchesLoading = true;
    const page = Math.floor(first / this.rowsPerPageMarches);

    this.marcheService.getAllMarches({ numero: this.currentMarcheFilter }, page, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        this.marchesOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = [...this.marchesOptions];
        this.marchesLoading = false;
      },
      error: () => this.marchesLoading = false
    });
  }

  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentMarcheFilter = query;

    if (this.filterTimeout) clearTimeout(this.filterTimeout);

    this.filterTimeout = setTimeout(() => {
      this.initializeMarches(query);
    }, 400);
  }

  // --- LOGIQUE TABLE PRINCIPALE ---

  loadPrefiLines(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField as string || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: SpecifiqueLineSearchDto = {
      marcheId: this.searchForm.value.marcheId,
      banqueId: this.searchForm.value.banqueId
    };

    this.specifiqueFunctionalLigneService.getPrefi(searchCriteria, page, size, sort).subscribe({
      next: (res) => {
        this.prefiLines = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des lignes.' });
      }
    });
  }

  loadBanquesSimple(): void {
    // Chargement global des banques car elles sont généralement peu nombreuses
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').subscribe(res => {
      this.banques = res.content.map(b => ({ label: b.nom, value: b.id }));
    });
  }

  clearFilters(): void {
    this.currentMarcheFilter = '';
    this.searchForm.reset({ marcheId: null, banqueId: null });
    this.initializeMarches();
  }
}
