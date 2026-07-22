import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { MessageService, ConfirmationService, MenuItem, ScrollerOptions } from 'primeng/api';
import { DropdownModule } from "primeng/dropdown";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { ToastModule } from "primeng/toast";
import { TagModule } from "primeng/tag";
import { InputSwitchModule } from "primeng/inputswitch";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { TooltipModule } from "primeng/tooltip";

// Services & Enums
import { PrefinancementService } from '../../service/PrefinancementService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from "../../../../auth/services/auth.service";
import { Type } from '../../enums/Type';
import { Status } from '../../enums/Status';

// DTOs
import { PrefinancementResponseDTO } from '../../dtos/PrefinancementResponseDTO';
import { PrefinancementSearchDTO } from '../../dtos/PrefinancementSearchDTO';

@Component({
  selector: 'app-prefinancements-list',
  standalone: true,
  templateUrl: './prefinancements-list.component.html',
  styleUrls: ['./prefinancements-list.component.scss'],
  imports: [
    CommonModule,
    TableModule,
    DropdownModule,
    BreadcrumbModule,
    ConfirmDialogModule,
    ToastModule,
    FormsModule,
    ReactiveFormsModule,
    TagModule,
    InputSwitchModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    RouterLink,
    DatePipe,
    DecimalPipe
  ],
  providers: [MessageService, ConfirmationService]
})
export class PrefinancementsListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  prefinancements: PrefinancementResponseDTO[] = [];
  totalRecords = 0;
  loading = true;

  // Dropdown Marchés (Lazy + Virtual Scroll + Server Search)
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

  // Autres données dropdowns
  banques: any[] = [];
  typeOptions: any[] = [];
  statusOptions: any[] = [];
  items: MenuItem[] = [];

  /** Indicateur de chargement du bouton "Exporter PDF". */
  exporting = false;

  private readonly fb = inject(FormBuilder);
  private readonly prefinancementService = inject(PrefinancementService);
  private readonly banqueService = inject(BanqueService);
  private readonly marcheService = inject(MarcheService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', routerLink: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', label: 'Préfinancements' },
    ];

    this.initForm();
    this.loadTypes();
    this.loadStatuses();
    this.loadBanquesSimple();
    this.initializeMarches();

    // Surveillance des changements de filtres avec debounce
    this.searchForm.valueChanges.subscribe(() => {
      if (this.table) {
        this.table.reset();
      }
    });
  }

  initForm(): void {
    this.searchForm = this.fb.group({
      marcheId: [null],
      banqueId: [null],
      type: [null],
      status: [null]
    });
  }

  // --- LOGIQUE DROPDOWN MARCHÉS ---

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
    this.marcheFilterTimeout = setTimeout(() => {
      this.initializeMarches(query);
    }, 400);
  }

  // --- LOGIQUE TABLE PRINCIPALE ---

  loadPrefinancements(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: PrefinancementSearchDTO = {
      ...this.searchForm.value
    };

    this.prefinancementService.getAllPrefinancements(searchCriteria, page, size, sort)
      .subscribe({
        next: (res) => {
          this.prefinancements = res.content;
          this.totalRecords = res.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  loadBanquesSimple(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').subscribe(res => {
      this.banques = res.content.map(b => ({ label: b.nom, value: b.id }));
    });
  }

  loadTypes() {
    this.typeOptions = [
      { label: 'REMBOURSEMENT', value: Type.REMBOURSEMENT },
      { label: 'DEBLOCAGE', value: Type.DEBLOCAGE }
    ];
  }

  loadStatuses() {
    this.statusOptions = [
      { label: 'Prévu', value: Status.PREVU },
      { label: 'Payé', value: Status.PAYE }
    ];
  }

  clearFilters() {
    this.currentMarcheFilter = '';
    this.searchForm.reset();
    this.initializeMarches();
  }

  // --- ACTIONS ---

  confirmToggleStatus(pref: PrefinancementResponseDTO) {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir changer le statut de ce préfinancement ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.prefinancementService.toggleStatus(pref.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
            this.table.reset();
          }
        });
      }
    });
  }

  deletePrefinancement(pref: PrefinancementResponseDTO) {
    this.confirmationService.confirm({
      message: `Supprimer le préfinancement de ${pref.montant} MAD ?`,
      header: 'Suppression',
      icon: 'pi pi-trash',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.prefinancementService.deletePrefinancement(pref.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Supprimé avec succès.' });
            this.table.reset();
          }
        });
      }
    });
  }

  /**
   * Exporte en PDF l'intégralité des préfinancements correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: PrefinancementSearchDTO = { ...this.searchForm.value };
    this.prefinancementService.exportPdf(searchCriteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `prefinancements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      },
    });
  }

  protected readonly Status = Status;
}
