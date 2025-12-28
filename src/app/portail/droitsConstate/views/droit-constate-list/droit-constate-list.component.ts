import { Component, OnInit, ViewChild } from '@angular/core';
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
    TagModule, InputSwitchModule, TooltipModule, DatePipe, DecimalPipe
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

  constructor(
    private fb: FormBuilder,
    private droitConstateService: DroitConstateService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    public authService: AuthService,
  ) {}

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
      status: [null]
    });
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
    const sort = `${event.sortField ?? 'dateEmission'},${event.sortOrder === 1 ? 'asc' : 'desc'}`;

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
    this.searchForm.reset({ marcheId: null, banqueId: null, type: null, status: null });
    this.initializeMarches();
  }

  // ... (Garder toggleStatus et deleteDroitConstate tels quels)

  confirmToggleStatus(dc: DroitConstateResponseDto){
    const confirmationMessage = `Êtes-vous sûr de vouloir ${dc.status==Status.PAYE ? 'désactiver' : 'activer'} ce droit constaté ?`;
    this.confirmationService.confirm({
      message: confirmationMessage,
      header: `Confirmation de ${dc.status ? 'désactivation' : 'activation'}`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: dc.status ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.toggleStatus(dc);
      }
    });
  }

  toggleStatus(dc: DroitConstateResponseDto) {
    if (dc.status === Status.PAYE) return;
      this.droitConstateService.toggleStatus(dc.id).subscribe({
        next: () => {
          this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.'});
          this.loadDroitsConstates(this.table.createLazyLoadMetadata());
        },
        error: () => {
          this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.'});
        }
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
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Impossible de supprimer le droit constaté.'
            });
          }
        });
      }
    });
  }
  protected readonly Status = Status;
  protected readonly Type = Type;
}
