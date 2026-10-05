import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from "@angular/common";
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from "@angular/forms";
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

// Services & DTOs
import { DroitConstateService } from "../../service/DroitConstateService";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from "../../../../auth/services/auth.service";
import { Status } from "../../../prefinancements/enums/Status";
import { Type } from "../../../prefinancements/enums/Type";
import {DroitConstateResponseDto} from "../../dtos/DroitConstateResponseDto";

@Component({
  selector: 'app-droit-constate-list',
  standalone: true,
  templateUrl: './droit-constate-list.component.html',
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, RouterLink,
    TableModule, ButtonModule, RippleModule, DropdownModule,
    BreadcrumbModule, ConfirmDialogModule, ToastModule,
    TagModule, InputSwitchModule, TooltipModule, DatePipe, DecimalPipe, CalendarModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class DroitConstateListComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  searchForm!: FormGroup; // Utilisation de Reactive Forms
  droitsConstates: any[] = [];
  totalRecords = 0;
  loading = true;
  items: MenuItem[] = [];

  // Configuration Marchés
  marchesOptions: any[] = [];
  marchesLoading = false;
  currentMarcheFilter = '';
  rowsPerPageMarches = 10;
  private marcheFilterTimeout: any;

  // Configuration du scroller identique à votre exemple de référence
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
  private readonly droitConstateService = inject(DroitConstateService);
  private readonly banqueService = inject(BanqueService);
  private readonly marcheService = inject(MarcheService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  public readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', routerLink: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', label: 'Droits constatés' },
    ];

    this.initForm();
    this.initializeMarches(); // On charge les premiers marchés
    this.loadBanquesSimple(); // On charge les banques

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

  // --- LOGIQUE MARCHÉS (Inspirée de votre exemple de référence) ---

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

  loadDroitsConstates(event: TableLazyLoadEvent) {
    this.loading = true;
    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sort = `${event.sortField ?? 'id'},${event.sortOrder === 1 ? 'asc' : 'desc'}`;

    // On utilise les valeurs du formulaire réactif
    const criteria = this.searchForm.value;

    this.droitConstateService.getAllDroitConstates(criteria, page, size, sort).subscribe({
      next: (res) => {
        this.droitsConstates = res.content;
        this.totalRecords = res.page.totalElements;
        this.loading = false;
      },
      error: () => this.loading = false
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

  // ... (Garder toggleStatus et deleteDroitConstate tels quels)

  confirmToggleStatus(dc: DroitConstateResponseDto){
    const toPrevu = dc.status === Status.PAYE;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir marquer ce droit constaté comme ${toPrevu ? 'prévu' : 'payé'} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: toPrevu ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleStatus(dc);
      },
      reject: () => this.loadDroitsConstates(this.table.createLazyLoadMetadata())
    });
  }

  toggleStatus(dc: DroitConstateResponseDto) {
    this.droitConstateService.toggleStatus(dc.id).subscribe({
      next: () => {
        this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.'});
        this.loadDroitsConstates(this.table.createLazyLoadMetadata());
      },
      error: () => this.loadDroitsConstates(this.table.createLazyLoadMetadata())
    });
  }

  /** Delete a droit constaté */
  deleteDroitConstate(dc: DroitConstateResponseDto) {
    this.confirmationService.confirm({
      message: `Supprimer le droit constaté d'un montant de ${dc.montant} MAD ?`,
      accept: () => {
        this.droitConstateService.deleteDroitConstate(dc.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Droit constaté supprimé.'});
            this.loadDroitsConstates(this.table.createLazyLoadMetadata());
          },
          error: () => {}
        });
      }
    });
  }
  /**
   * Exporte en PDF l'intégralité des droits constatés correspondant aux filtres actuellement
   * appliqués — pas seulement la page affichée à l'écran.
   */
  exportPdf(): void {
    this.exporting = true;
    const criteria = this.searchForm.value;
    this.droitConstateService.exportPdf(criteria).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `droits_constates_${new Date().toISOString().slice(0, 10)}.pdf`;
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
