import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// PrimeNG
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { DropdownModule } from "primeng/dropdown";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { ToastModule } from "primeng/toast";
import { TagModule } from "primeng/tag";
import { InputSwitchModule } from "primeng/inputswitch";
import { TooltipModule } from 'primeng/tooltip';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';

// Services & Enums
import { DecompteService } from "../../service/DecompteService";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { Status } from "../../../prefinancements/enums/Status";
import { DecompteResponseDto } from "../../dtos/DecompteResponseDto";
import {AuthService} from "../../../../auth/services/auth.service";

@Component({
  selector: 'app-decomptes-list',
  standalone: true,
  templateUrl: './decomptes-list.component.html',
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, RouterLink,
    TableModule, ButtonModule, RippleModule, DropdownModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule,
    TagModule, InputSwitchModule, TooltipModule, DatePipe, DecimalPipe,
    CalendarModule, InputNumberModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class DecomptesListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  decomptes: DecompteResponseDto[] = [];
  isAdmin = false;
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // --- Configuration Marchés (Server-side Search + Virtual Scroll) ---
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

  showFilterPanel = true;
  exporting = false;

  constructor(
    private fb: FormBuilder,
    private decompteService: DecompteService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/decomptes', label: 'Les Décomptes' },
    ];

    this.initForm();
    this.initializeMarches();
    this.loadBanquesSimple();

    // Rechargement automatique de la table lors du changement des filtres
    this.searchForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      if (this.table) this.table.reset();
    });

    this.isAdmin=this.authService.isAdmin()
  }

  initForm(): void {
    this.searchForm = this.fb.group({
      marcheId:       [null],
      banqueId:       [null],
      status:         [null],
      dateValeurFrom: [null],
      dateValeurTo:   [null],
      montantMin:     [null],
      montantMax:     [null]
    });
  }

  // --- LOGIQUE DROPDOWN MARCHÉS (Lazy + Filter) ---

  initializeMarches(filter: string = ''): void {
    this.marchesLoading = true;
    this.marcheService.getAllMarches({ numero: filter }, 0, this.rowsPerPageMarches, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));
        // Initialise le tableau avec la taille totale pour le scroll
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
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));
        this.marchesOptions.splice(first, data.length, ...data);
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

  loadDecomptes(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sort = `${event.sortField ?? 'dateValeur'},${event.sortOrder === 1 ? 'asc' : 'desc'}`;

    const criteria = this.searchForm.value;

    this.decompteService.getAllDecomptes(criteria, page, size, sort).subscribe({
      next: (res) => {
        this.decomptes = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearFilters() {
    this.currentMarcheFilter = '';
    this.searchForm.reset({
      marcheId: null, banqueId: null, status: null,
      dateValeurFrom: null, dateValeurTo: null,
      montantMin: null, montantMax: null
    });
    this.initializeMarches();
  }

  toggleFilterPanel() {
    this.showFilterPanel = !this.showFilterPanel;
  }

  get activeFilterCount(): number {
    return this.activeFilters.length;
  }

  get activeFilters(): { label: string; field: string }[] {
    const v = this.searchForm.value;
    const filters: { label: string; field: string }[] = [];

    if (v.marcheId) {
      const opt = this.marchesOptions.find((m: any) => m.value === v.marcheId);
      filters.push({ label: opt?.label ?? 'Marché', field: 'marcheId' });
    }
    if (v.banqueId) {
      const opt = this.banquesOptions.find((b: any) => b.value === v.banqueId);
      filters.push({ label: opt?.label ?? 'Banque', field: 'banqueId' });
    }
    if (v.status) {
      filters.push({ label: v.status === 'PAYE' ? 'Payé' : 'Prévu', field: 'status' });
    }
    if (v.dateValeurFrom || v.dateValeurTo) {
      const from = v.dateValeurFrom ? this.formatDateChip(v.dateValeurFrom) : '…';
      const to   = v.dateValeurTo   ? this.formatDateChip(v.dateValeurTo)   : '…';
      filters.push({ label: `${from} → ${to}`, field: 'dateValeur' });
    }
    if (v.montantMin != null || v.montantMax != null) {
      const min = v.montantMin != null ? Number(v.montantMin).toLocaleString('fr-FR') : '0';
      const max = v.montantMax != null ? Number(v.montantMax).toLocaleString('fr-FR') : '∞';
      filters.push({ label: `${min} → ${max} MAD`, field: 'montant' });
    }
    return filters;
  }

  removeFilter(field: string) {
    if (field === 'dateValeur') {
      this.searchForm.patchValue({ dateValeurFrom: null, dateValeurTo: null });
    } else if (field === 'montant') {
      this.searchForm.patchValue({ montantMin: null, montantMax: null });
    } else {
      this.searchForm.patchValue({ [field]: null });
    }
  }

  private formatDateChip(date: Date): string {
    const d = String(date.getDate()).padStart(2, '0');
    const m = String(date.getMonth() + 1).padStart(2, '0');
    return `${d}/${m}/${date.getFullYear()}`;
  }

  exportPdf(): void {
    this.exporting = true;
    const criteria = this.searchForm.value;
    this.decompteService.exportPdf(criteria).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `etat_decomptes_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  toggleStatus(decompte: DecompteResponseDto) {
    this.decompteService.toggleStatus(decompte.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
        this.table.reset();
      },
      error: () => {}
    });
  }

  deleteDecompte(decompte: DecompteResponseDto) {
    this.confirmationService.confirm({
      message: `Supprimer la décompte d'un montant de ${decompte.montant} MAD ?`,
      accept: () => {
        this.decompteService.deleteDecompte(decompte.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Décompte supprimée.' });
            this.table.reset();
          }
        });
      }
    });
  }

  protected readonly Status = Status;
}
