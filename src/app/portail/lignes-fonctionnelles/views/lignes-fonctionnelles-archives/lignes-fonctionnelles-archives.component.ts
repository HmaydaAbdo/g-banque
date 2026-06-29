import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { BreadcrumbModule } from "primeng/breadcrumb";
import { ButtonDirective } from "primeng/button";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DatePipe, DecimalPipe, NgClass, NgIf } from "@angular/common";
import { DropdownModule } from "primeng/dropdown";
import { PaginatorModule } from "primeng/paginator";
import { ConfirmationService, MenuItem, MessageService, PrimeTemplate, ScrollerOptions } from "primeng/api";
import { FormBuilder, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { Ripple } from "primeng/ripple";
import {Router, RouterLinkActive} from "@angular/router";
import { Table, TableLazyLoadEvent, TableModule } from "primeng/table";
import { TagModule } from "primeng/tag";
import { ToastModule } from "primeng/toast";
import { TooltipModule } from "primeng/tooltip";
import { LigneFonctionnelleResponse } from "../../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { NatureLigneFonctionnelleService } from "../../services/nature-ligne-fonctionnelle.service";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { LigneFonctionnelleSearchCriteria } from "../../dtos/LigneFonctionnelleSearchCriteria";

// RxJS
import { Subject, BehaviorSubject, combineLatest, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, switchMap, catchError, tap } from 'rxjs/operators';
import {LineNavigationService, LineType} from "../../services/LineNavigationService";

const MARCHE_PAGE_SIZE = 10;
const DEBOUNCE_TIME = 300;

@Component({
  selector: 'app-lignes-fonctionnelles-archives',
  standalone: true,
  imports: [
    BreadcrumbModule,
    ButtonDirective,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass
  ],
  templateUrl: './lignes-fonctionnelles-archives.component.html',
  styleUrl: './lignes-fonctionnelles-archives.component.scss',
  providers: [ ConfirmationService]
})
export class LignesFonctionnellesArchivesComponent implements OnInit, OnDestroy {

  // ========== REACTIVE STREAMS ==========
  private readonly destroy$ = new Subject<void>();
  private readonly selectedBanqueId$ = new BehaviorSubject<number | null>(null);
  private readonly marcheFilter$ = new BehaviorSubject<string>('');

  // ========== TABLE ==========
  @ViewChild('dt') dt: Table | undefined;
  lignesFonctionnelles: LigneFonctionnelleResponse[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  first: number = 0;
  rows: number = 10;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;
  expandedRows = {};

  // ========== FORMS ==========
  searchForm!: FormGroup;

  // ========== DROPDOWNS ==========
  natureLigneOptions: { label: string; value: number | null }[] = [];
  banquesOptions: { label: string; value: number | null }[] = [];
  marchesOptions: any[] = [];

  // ========== MARCHÉ LAZY LOADING ==========
  loadingMarches = false;
  marchesScrollerOptions: ScrollerOptions;

  // ========== UI ==========
  items: MenuItem[] = [];

  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private router: Router,
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
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-archived', label: "Les Lignes fonctionnelles Archivées" }
    ];
  }

  private initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
      actif: [false]
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
    // Charger tous les marchés au démarrage
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

    // Tracker les changements de banque
    this.searchForm.get('banqueId')!.valueChanges.pipe(
      takeUntil(this.destroy$),
      tap(banqueId => {
        this.selectedBanqueId$.next(banqueId);
      })
    ).subscribe();
  }

  private setupMarcheFilterStream(): void {
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

    this.marchesOptions = Array.from({ length: total + 1 }).map(() => ({}));
    this.marchesOptions.splice(0, data.length, ...data);
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / MARCHE_PAGE_SIZE);

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
    this.sortField = event.sortField || 'id';
    this.sortOrder = event.sortOrder === 1 ? 1 : -1;

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
          console.error('Error fetching functional lignes:', err);
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
      actif: false
    });
  }

  redirectToLineDetails(lineId: number, label: string): void {
    // Pour les lignes GÉNÉRALES, on utilise LineType.GENERAL
    this.lineNavigationService.redirectToLineDetails(lineId, label, LineType.GENERAL);
  }

}
