import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService, ConfirmationService } from 'primeng/api';
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

import { Role } from "../../../../auth/enum/Role";
import { AuthService } from "../../../../auth/services/auth.service";
import { LigneFonctionnelleSpecifiqueResponse } from "../../dtos/LigneFonctionnelleSpecifiqueResponse";
import { LigneFonctionnelleSpecifiqueSearchCriteria } from "../../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import { SpecificFunctionalLigneService } from "../../service/SpecificFunctionalLigneService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { MarcheService } from "../../../Marches/service/marche-service.service";

import {
  NatureLigneFonctionnelleResponse
} from "../../../lignes-fonctionnelles/dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse";
import {
  NatureLigneFonctionnelleService
} from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";

@Component({
  selector: 'app-spefic-ligne-encours',
  templateUrl: './spefic-ligne-encours.component.html',
  styleUrls: ['./spefic-ligne-encours.component.scss'],
  standalone: true,
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
export class SpeficLigneEncoursComponent implements OnInit {

  @ViewChild('dt') dt: Table | undefined;

  lignesFonctionnellesSpecifiques: LigneFonctionnelleSpecifiqueResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number = 10;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  searchForm!: FormGroup;
  statutOptions: any[] = [
    { label: 'Tous', value: null },
    { label: 'En cours', value: false },
    { label: 'Expirées', value: true }
  ];
  banquesOptions: { label: string; value: number | null }[] = [];
  marchesOptions: { label: string; value: number | null }[] = [];
  natureLigneOptions: { label: string; value: number | null }[] = [];
  items: MenuItem[] | undefined;

  marchesLoading: boolean = false;
  marchesTotalRecords: number = 0;

  // The type is now inferred, removing the TS2724 error.
  marchesScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: (event: any) => this.loadMarchesForDropdown(event)
  };

  constructor(
    private specificLigneService: SpecificFunctionalLigneService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" }, { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-specifiques-encours', label: "Les Lignes Fonctionnelles Spécifiques Encours" }];
    this.initSearchForm();
    this.setupFormListeners();
    this.loadFilterOptions();
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
      actif: [true],
      isExpired: [null]
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

  loadMarchesForDropdown(event: { first: number, last: number, filter?: string }): void {
    this.marchesLoading = true;
    const page = Math.floor(event.first / 10);
    const size = 10;
    const sort = 'nom,asc';

    this.marcheService.getAllMarchesForLazyLoad(page, size, sort).subscribe({
      next: (pageResponse) => {
        const newItems = pageResponse.content.map(marche => ({ label: marche.numero, value: marche.id! }));

        if (event.first === 0) {
          this.marchesOptions = [{ label: 'Tous les marchés', value: null }, ...newItems];
        } else {
          this.marchesOptions = [...this.marchesOptions, ...newItems];
        }

        this.marchesTotalRecords = pageResponse.page.totalElements;
        this.marchesLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des marchés pour le filtre.', life: 3000 });
        console.error('Error loading markets for filter:', err);
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
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des lignes fonctionnelles spécifiques.', life: 3000 });
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
      actif: true,
      isExpired:false,
    });
  }

  deleteLigneFonctionnelleSpecifique(ligne: LigneFonctionnelleSpecifiqueResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la ligne fonctionnelle spécifique avec la nature "${ligne.natureLigneFonctionnelleLabel}" pour la banque "${ligne.banqueNom}" et le marché "${ligne.marcheNom}" ?`,
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.specificLigneService.deleteLigneFonctionnelleSpecifique(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle Spécifique supprimée avec succès.', life: 3000 });
              this.reloadTableAfterAction();
            },
            error: (error) => {
              console.error('Erreur lors de la suppression de la ligne fonctionnelle spécifique:', error);
              const errorMessage = error.error?.message || 'Échec de la suppression de la ligne fonctionnelle spécifique.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }

  desactiver(ligne: LigneFonctionnelleSpecifiqueResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir désactiver la ligne fonctionnelle spécifique avec la nature "${ligne.natureLigneFonctionnelleLabel}" pour la banque "${ligne.banqueNom}" et le marché "${ligne.marcheNom}" ?`,
      header: 'Confirmation de Désactivation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel:"Oui",
      rejectLabel:"Non",
      accept: () => {
        this.loading = true;
        this.specificLigneService.deactivateLigneFonctionnelle(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle Spécifique désactivée avec succès.', life: 3000 });
              this.reloadTableAfterAction();
            },
            error: (error) => {
              console.error('Erreur lors de la désactivation de la ligne fonctionnelle spécifique:', error);
              const errorMessage = error.error?.message || 'Échec de la désactivation de la ligne fonctionnelle spécifique.';
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

  private reloadTableAfterAction(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      this.loadLignesFonctionnellesSpecifiques({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }
}
