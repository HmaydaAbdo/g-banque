import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { SpeedDialModule } from 'primeng/speeddial';

// RxJS
import { Subject, BehaviorSubject, combineLatest, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, switchMap, catchError, tap } from 'rxjs/operators';

// Services & DTOs
import { LigneFonctionnelleResponse } from "../../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleSearchCriteria } from "../../dtos/LigneFonctionnelleSearchCriteria";
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { NatureLigneFonctionnelleService } from "../../services/nature-ligne-fonctionnelle.service";
import { AuthService } from "../../../../auth/services/auth.service";
import {LineNavigationService, LineType} from "../../services/LineNavigationService";


const MARCHE_PAGE_SIZE = 10;
const DEBOUNCE_TIME = 300;

@Component({
  selector: 'app-lignes-fonctionnelles-encours',
  templateUrl: './lignes-fonctionnelles.component.html',
  styleUrls: ['./lignes-fonctionnelles.component.scss'],
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, DecimalPipe, DatePipe,
    RouterLink, RouterLinkActive, TableModule, InputTextModule, ButtonModule,
    RippleModule, ToastModule, ConfirmDialogModule, ToolbarModule, TagModule,
    TooltipModule, DropdownModule, InputNumberModule, CalendarModule,
    InputSwitchModule, BreadcrumbModule, SpeedDialModule
  ],
  providers: [ ConfirmationService]
})
export class LignesFonctionnellesComponent implements OnInit, OnDestroy {

  // ========== REACTIVE STREAMS ==========
  private readonly destroy$ = new Subject<void>();
  private readonly selectedBanqueId$ = new BehaviorSubject<number | null>(null);
  private readonly marcheFilter$ = new BehaviorSubject<string>('');

  // ========== TABLE ==========
  @ViewChild('dt') dt: Table | undefined;
  lignesFonctionnelles: LigneFonctionnelleResponse[] = [];
  totalRecords = 0;
  loading = false;
  first = 0;
  rows = 10;
  sortField: string | string[] | null | undefined;
  sortOrder: number | null | undefined;
  expandedRows = {};

  // ========== FORMS ==========
  searchForm!: FormGroup;

  // ========== DROPDOWNS ==========
  natureLigneOptions: Array<{ label: string; value: number | null }> = [];
  banquesOptions: Array<{ label: string; value: number | null }> = [];
  marchesOptions: any[] = [];
  statutOptions = [
    { label: 'Tous', value: null },
    { label: 'En cours', value: false },
    { label: 'Expirées', value: true }
  ];

  // ========== MARCHÉ LAZY LOADING ==========
  loadingMarches = false;
  marchesScrollerOptions: ScrollerOptions;

  // ========== UI ==========
  items: MenuItem[] = [];

  constructor(
    private readonly ligneFonctionnelleService: LigneFonctionnelleService,
    private readonly banqueService: BanqueService,
    private readonly marcheService: MarcheService,
    private readonly natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly messageService: MessageService,
    private readonly confirmationService: ConfirmationService,
    private readonly authService: AuthService,
    private readonly lineNavigationService: LineNavigationService

  ) {
    this.marchesScrollerOptions = {
      delay: 250,
      showLoader: true,
      lazy: true,
      onLazyLoad: this.onLazyLoadMarches.bind(this)
    };
  }

  // ========== LIFECYCLE ==========

  ngOnInit(): void {
    this.initBreadcrumb();
    this.initSearchForm();
    this.loadStaticDropdowns();
    this.setupReactiveStreams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ========== TEMPLATE HELPERS ==========

  getMarchesExtraTooltip(marches: any[]): string {
    return marches.slice(2).map((m: any) => m.numero).join(', ');
  }

  // ========== INITIALIZATION ==========

  private initBreadcrumb(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label: "Lignes fonctionnelles" }
    ];
  }

  private initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
      actif: [true],
      isExpired: [null]
    });
  }

  private loadStaticDropdowns(): void {
    combineLatest([
      this.loadBanques(),
      this.loadNatureLignes()
    ]).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ([banques, natures]) => {
        this.banquesOptions = banques;
        this.natureLigneOptions = natures;
      },
      error: (err) => {
        console.error('Error loading dropdowns:', err);
      }
    });
  }

  private loadBanques() {
    return this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').pipe(
      takeUntil(this.destroy$),
      catchError(err => {
        console.error('Error loading banques:', err);
        return of({ content: [] });
      }),
      switchMap(response => {
        const options = [{ label: 'Toutes les banques', value: null }];
        response.content.forEach((banque: any) => {
          options.push({ label: banque.nom, value: banque.id });
        });
        return of(options);
      })
    );
  }

  private loadNatureLignes() {
    return this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().pipe(
      takeUntil(this.destroy$),
      catchError(err => {
        console.error('Error loading natures:', err);
        return of([]);
      }),
      switchMap(natures => {
        const options = [{ label: 'Toutes les natures', value: null }];
        natures.forEach((nature: any) => {
          options.push({ label: nature.label, value: nature.id });
        });
        return of(options);
      })
    );
  }

  // ========== REACTIVE STREAMS ==========

  private setupReactiveStreams(): void {
    this.setupSearchFormStream();
    this.setupBanqueChangeStream();
    this.setupMarcheFilterStream();
  }

  private setupSearchFormStream(): void {
    this.searchForm.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(500),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.onSearch();
    });
  }

  private setupBanqueChangeStream(): void {
    // ✅ Charger tous les marchés au démarrage (sans filtre banque)
    this.loadAllMarches(0).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response) => {
        this.initializeMarchesOptions(response);
      },
      error: (err) => {
        console.error('Error loading marchés:', err);
        this.marchesOptions = [];
      }
    });

    // ✅ Optionnel: Si une banque est sélectionnée, filtrer les marchés
    this.searchForm.get('banqueId')!.valueChanges.pipe(
      takeUntil(this.destroy$),
      tap(banqueId => {
        this.selectedBanqueId$.next(banqueId);
        // Ne pas reset marcheId pour permettre la recherche indépendante
      })
    ).subscribe();
  }

  private setupMarcheFilterStream(): void {
    // ✅ Recherche réactive avec debounce (sans dépendance obligatoire à banqueId)
    this.marcheFilter$.pipe(
      takeUntil(this.destroy$),
      debounceTime(DEBOUNCE_TIME),
      distinctUntilChanged(),
      switchMap(filter => {
        const banqueId = this.selectedBanqueId$.value;
        return this.loadAllMarches(0, filter, banqueId);
      })
    ).subscribe({
      next: (response) => {
        this.initializeMarchesOptions(response);
      },
      error: (err) => {
        console.error('Error filtering marchés:', err);
      }
    });
  }

  // ========== MARCHÉ LOADING ==========

  /**
   * ✅ Charge tous les marchés, avec filtres optionnels
   * @param page Page à charger
   * @param filter Filtre par numéro (optionnel)
   * @param banqueId Filtre par banque (optionnel)
   */
  private loadAllMarches(page: number, filter: string = '', banqueId?: number | null) {
    const criteria: any = {};

    if (filter) {
      criteria.numero = filter;
    }

    if (banqueId) {
      criteria.banqueId = banqueId;
    }

    return this.marcheService.getAllMarches(
      criteria,
      page,
      MARCHE_PAGE_SIZE,
      'numero,asc'
    ).pipe(
      catchError(err => {
        console.error('Error loading marchés:', err);
        return of({ content: [], page: { totalElements: 0 } });
      })
    );
  }

  private initializeMarchesOptions(response: any): void {
    const total = response.page.totalElements;
    const data = [{ label: 'Tous les marchés', value: null }];

    response.content.forEach((m: any) => {
      data.push({ label: m.numero, value: m.id });
    });

    // Pour virtual scroll, créer des placeholders
    this.marchesOptions = Array.from({ length: total + 1 }).map(() => ({}));
    this.marchesOptions.splice(0, data.length, ...data);
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / MARCHE_PAGE_SIZE);

    // Skip si déjà chargé
    if (this.marchesOptions[first]?.value !== undefined) {
      return;
    }

    this.loadingMarches = true;
    const banqueId = this.selectedBanqueId$.value;
    const filter = this.marcheFilter$.value;

    this.loadAllMarches(page, filter, banqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const newItems = response.content.map((m: any) => ({ label: m.numero, value: m.id }));
          const updatedOptions = [...this.marchesOptions];
          updatedOptions.splice(first, newItems.length, ...newItems);
          this.marchesOptions = updatedOptions;
          this.loadingMarches = false;
        },
        error: () => {
          this.loadingMarches = false;
        }
      });
  }

  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.marcheFilter$.next(query);
  }

  // ========== TABLE DATA LOADING ==========

  loadLignesFonctionnelles(event: TableLazyLoadEvent): void {
    this.loading = true;

    this.first = event.first !== undefined ? event.first : 0;
    this.rows = (event.rows !== undefined ? event.rows : 10) as number;
    this.sortField = event.sortField;
    this.sortOrder = event.sortOrder;

    const page = this.first / this.rows;

    const actualSortField = Array.isArray(this.sortField) ? this.sortField[0] : this.sortField;
    const sort = `${actualSortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`;

    const searchCriteria: LigneFonctionnelleSearchCriteria = this.searchForm.value;

    this.ligneFonctionnelleService
      .getCurrentLigneFonctionnelles(searchCriteria, page, this.rows, sort)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.lignesFonctionnelles = data.content;
          this.totalRecords = data.page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error fetching lignes:', err);
          this.loading = false;
        }
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.dt.filterTimeout = 0;
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      this.loadLignesFonctionnelles({
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
      isExpired: false
    });
  }

  // ========== ACTIONS ==========

  desactiver(ligne: LigneFonctionnelleResponse): void {
    if (!ligne) {
      console.error('Ligne is null');
      return;
    }

    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir désactiver la ligne "${ligne.natureLigneFonctionnelleLabel}" avec "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Désactivation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: "Oui",
      rejectLabel: "Non",
      accept: () => {
        this.loading = true;
        this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Ligne désactivée avec succès',
                life: 3000
              });
              this.reloadTableAfterAction();
            },
            error: (error) => {
              console.error('Error deactivating ligne:', error);
              this.loading = false;
            }
          });
      }
    });
  }

  deleteLigneFonctionnelle(ligne: LigneFonctionnelleResponse): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer la ligne "${ligne.natureLigneFonctionnelleLabel}" pour "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.ligneFonctionnelleService.deleteLigneFonctionnelle(ligne.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Ligne supprimée avec succès',
                life: 3000
              });
              this.reloadTableAfterAction();
            },
            error: (error) => {
              console.error('Error deleting ligne:', error);
              this.loading = false;
            }
          });
      }
    });
  }

  // ========== HELPERS ==========

  isAdmin(): boolean {
    return this.authService.isSystemAdmin();
  }

  private reloadTableAfterAction(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    } else {
      this.loadLignesFonctionnelles({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      });
    }
  }

  redirectToLineDetails(lineId: number, label: string): void {
    // Pour les lignes GÉNÉRALES, on utilise LineType.GENERAL
    this.lineNavigationService.redirectToLineDetails(lineId, label, LineType.GENERAL);
  }
}
