import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ToastModule } from "primeng/toast";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DropdownModule } from "primeng/dropdown";
import { RippleModule } from "primeng/ripple";
import { TooltipModule } from "primeng/tooltip";
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Services & DTOs
import { SpecificFunctionalLigneService } from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import { MarcheService } from "../../../../Marches/service/marche-service.service";
import { BanqueService } from "../../../../comptes-bancaire/services/BanqueService";
import { SpecifiqueLineSearchDto } from "../../../../prefinancements/lignesPrefi/dtos/SpecifiqueLineSearchDto";
import { SpecificLigneResponseDto } from "../../../../specific-Functional-Lignes/dtos/SpecificLigneResponseDto";

@Component({
  selector: 'app-dc-lines-list',
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
    RouterLink,
    DatePipe,
    DecimalPipe
  ],
  providers: [MessageService],
  templateUrl: './dc-lines-list.component.html',
  styleUrl: './dc-lines-list.component.scss'
})
export class DcLinesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  dcLines: SpecificLigneResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Configuration Marchés (Lazy + Virtual Scroll + Server Filter)
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

  banques: any[] = [];

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
      { icon: 'pi pi-list', label: 'Lignes de Droits Constatés' }
    ];

    this.initForm();
    this.initializeMarches();
    this.loadBanquesSimple();

    // Surveillance des filtres avec debounce pour recharger la table
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

  // --- LOGIQUE DROPDOWN MARCHÉS (Server Side Search) ---

  initializeMarches(filter: string = ''): void {
    this.marchesLoading = true;
    this.marcheService.getAllMarches({ numero: filter }, 0, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        // Initialisation propre pour le virtual scroll
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
    this.marcheFilterTimeout = setTimeout(() => {
      this.initializeMarches(query);
    }, 400);
  }

  // --- LOGIQUE TABLE PRINCIPALE ---

  loadDcLines(event: TableLazyLoadEvent): void {
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

    this.specifiqueFunctionalLigneService.getDcLines(searchCriteria, page, size, sort).subscribe({
      next: (res) => {
        this.dcLines = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les lignes.' });
      }
    });
  }

  loadBanquesSimple(): void {
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
