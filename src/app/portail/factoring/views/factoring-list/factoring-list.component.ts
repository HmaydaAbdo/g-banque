import { Component, OnInit, ViewChild, inject } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from "@angular/forms";
import { CommonModule, DatePipe, DecimalPipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// PrimeNG
import { Table, TableLazyLoadEvent, TableModule } from "primeng/table";
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from "primeng/api";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { DropdownModule } from "primeng/dropdown";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { ToastModule } from "primeng/toast";
import { TagModule } from "primeng/tag";
import { InputSwitchModule } from "primeng/inputswitch";
import { TooltipModule } from "primeng/tooltip";
import { CalendarModule } from "primeng/calendar";

// Services & Enums
import { FactoringService } from "../../service/FactoringService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { MarcheService } from "../../../Marches/service/marche-service.service";
import { AuthService } from "../../../../auth/services/auth.service";
import { Status } from "../../../prefinancements/enums/Status";
import { Type } from "../../../prefinancements/enums/Type";

@Component({
  selector: 'app-factoring-list',
  standalone: true,
  templateUrl: './factoring-list.component.html',
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, RouterLink,
    TableModule, DropdownModule, BreadcrumbModule, ConfirmDialogModule,
    ToastModule, ButtonModule, RippleModule, TagModule, InputSwitchModule,
    TooltipModule, DatePipe, DecimalPipe, CalendarModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class FactoringListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup;
  factoring: any[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Configuration Marchés (Virtual Scroll + Lazy)
  marchesOptions: any[] = [];
  marchesLoading = false;
  currentMarcheFilter = '';
  rowsPerPageMarches = 10;
  private marcheFilterTimeout: any;

  // AJOUT : Configuration du scroller pour le virtual scroll lazy
  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  banquesOptions: any[] = [];

  /** Indicateur de chargement du bouton "Exporter PDF". */
  exporting = false;

  /** Affiche/masque le panneau de recherche par période (fermé par défaut pour ne pas surcharger la barre de filtres). */
  showDatePanel = false;

  private readonly fb = inject(FormBuilder);
  private readonly factoringService = inject(FactoringService);
  private readonly banqueService = inject(BanqueService);
  private readonly marcheService = inject(MarcheService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', routerLink: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', label: 'Factoring' },
    ];

    this.initForm();
    this.initializeMarches();
    this.loadBanquesSimple();

    // Recharger la table automatiquement quand les filtres changent
    this.searchForm.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      if (this.table) this.table.reset();
    });
  }

  initForm(): void {
    this.searchForm = this.fb.group({
      marcheId: [null],
      banqueId: [null],
      type: [null],
      status: [null],
      dateEmissionFrom: [null],
      dateEmissionTo: [null],
      dateOperationFrom: [null],
      dateOperationTo: [null],
      dateValeurFrom: [null],
      dateValeurTo: [null]
    });
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEmissionFrom || v.dateEmissionTo) count++;
    if (v.dateOperationFrom || v.dateOperationTo) count++;
    if (v.dateValeurFrom || v.dateValeurTo) count++;
    return count;
  }

  // --- LOGIQUE MARCHÉS ---
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
    // Si l'élément est déjà chargé, on ne fait rien
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

  // --- LOGIQUE TABLE ---
  loadFactoring(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const criteria = this.searchForm.value;

    this.factoringService.searchFactoring(criteria, page, size, sort).subscribe({
      next: (res) => {
        this.factoring = res.content;
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
      marcheId: null, banqueId: null, type: null, status: null,
      dateEmissionFrom: null, dateEmissionTo: null,
      dateOperationFrom: null, dateOperationTo: null,
      dateValeurFrom: null, dateValeurTo: null
    });
    this.initializeMarches();
  }

  confirmToggleStatus(factoring: any) {
    this.confirmationService.confirm({
      message: `Changer le statut de ce factoring ?`,
      header: `Confirmation`,
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.factoringService.toggleStatus(factoring.id).subscribe(() => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
          this.table.reset();
        });
      }
    });
  }

  deleteFactoring(factoring: any) {
    this.confirmationService.confirm({
      message: `Supprimer ce factoring d'un montant de ${factoring.montant} MAD ?`,
      header: 'Suppression',
      icon: 'pi pi-trash',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.factoringService.deleteFactoring(factoring.id).subscribe(() => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Supprimé avec succès.' });
          this.table.reset();
        });
      }
    });
  }

  /**
   * Exporte en PDF l'intégralité des factorings correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const criteria = this.searchForm.value;
    this.factoringService.exportPdf(criteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `factoring_${new Date().toISOString().slice(0, 10)}.pdf`;
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
  protected readonly Type = Type;
}
