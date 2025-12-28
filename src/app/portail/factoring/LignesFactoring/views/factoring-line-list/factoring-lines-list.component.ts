import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// PrimeNG
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ToastModule } from "primeng/toast";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DropdownModule } from "primeng/dropdown";
import { RippleModule } from "primeng/ripple";
import { TooltipModule } from "primeng/tooltip";

// Services & DTOs
import { SpecificFunctionalLigneService } from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import { MarcheService } from "../../../../Marches/service/marche-service.service";
import { BanqueService } from "../../../../comptes-bancaire/services/BanqueService";
import { SpecifiqueLineSearchDto } from "../../../../prefinancements/lignesPrefi/dtos/SpecifiqueLineSearchDto";
import { SpecificLigneResponseDto } from "../../../../specific-Functional-Lignes/dtos/SpecificLigneResponseDto";

@Component({
  selector: 'app-factoring-lines-list',
  standalone: true,
  imports: [
    CommonModule, TableModule, BreadcrumbModule, ButtonModule, ToastModule,
    ConfirmDialogModule, DropdownModule, FormsModule, ReactiveFormsModule,
    RippleModule, TooltipModule, RouterLink, DatePipe, DecimalPipe
  ],
  providers: [MessageService],
  templateUrl: './factoring-lines-list.component.html'
})
export class FactoringLinesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  factoringLines: SpecificLigneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Configuration Marchés (Lazy + Server Search)
  marchesOptions: any[] = [];
  marchesLoading = false;
  currentMarcheFilter = '';
  rowsPerPageMarches = 10;
  private marcheFilterTimeout: any;

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  banquesOptions: any[] = [];

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
      { icon: 'pi pi-list', label: 'Lignes de Factoring' }
    ];

    this.initForm();
    this.initializeMarches();
    this.loadBanquesSimple();

    // Surveillance des filtres pour reset la table
    this.searchForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      if (this.table) this.table.reset();
    });
  }

  initForm(): void {
    this.searchForm = this.fb.group({
      marcheId: [null],
      banqueId: [null]
    });
  }

  // --- LOGIQUE MARCHÉS (Server Side Search) ---

  initializeMarches(filter: string = ''): void {
    this.marchesLoading = true;
    this.marcheService.getAllMarches({ numero: filter }, 0, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));
        this.marchesOptions = Array.from({ length: total }).map(() => ({}));
        this.marchesOptions.splice(0, data.length, ...data);
        this.marchesOptions = [...this.marchesOptions];
        this.marchesLoading = false;
      },
      error: () => this.marchesLoading = false
    });
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
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
    if (this.marcheFilterTimeout) clearTimeout(this.marcheFilterTimeout);
    this.marcheFilterTimeout = setTimeout(() => this.initializeMarches(query), 400);
  }

  loadBanquesSimple(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').subscribe(res => {
      this.banquesOptions = res.content.map(b => ({ label: b.nom, value: b.id }));
    });
  }

  // --- LOGIQUE TABLE PRINCIPALE ---

  loadFactoringLines(event: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sort = `${event.sortField || 'id'},${event.sortOrder === 1 ? 'asc' : 'desc'}`;

    const searchCriteria: SpecifiqueLineSearchDto = this.searchForm.value;

    this.specifiqueFunctionalLigneService.getFactoringLines(searchCriteria, page, size, sort).subscribe({
      next: (res) => {
        this.factoringLines = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les lignes.' });
      }
    });
  }

  clearFilters(): void {
    this.currentMarcheFilter = '';
    this.searchForm.reset();
    this.initializeMarches();
  }
}
