import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive} from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService, ConfirmationService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { BreadcrumbModule } from 'primeng/breadcrumb';

// RxJS
import { Subject, BehaviorSubject, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, switchMap, catchError, tap } from 'rxjs/operators';

// Auth & Services
import { LigneFonctionnelleSpecifiqueResponse } from "../../dtos/LigneFonctionnelleSpecifiqueResponse";
import { LigneFonctionnelleSpecifiqueSearchCriteria } from "../../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import { SpecificFunctionalLigneService } from "../../service/SpecificFunctionalLigneService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { MarcheService } from "../../../Marches/service/marche-service.service";
import { NatureLigneFonctionnelleService } from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";
import {LineNavigationService, LineType} from "../../../lignes-fonctionnelles/services/LineNavigationService";

const MARCHE_PAGE_SIZE = 10;
const DEBOUNCE_TIME = 300;

@Component({
  selector: 'app-spefic-ligne-archivees',
  templateUrl: './spefic-ligne-archivees.component.html',
  styleUrls: ['./spefic-ligne-archivees.component.scss'],
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, DecimalPipe, DatePipe,
    RouterLink, TableModule, InputTextModule, ButtonModule, RippleModule,
    ToastModule, ConfirmDialogModule, ToolbarModule, TagModule, TooltipModule,
    DropdownModule, InputNumberModule, CalendarModule, InputSwitchModule,
    BreadcrumbModule, RouterLinkActive
  ],
  providers: [ ConfirmationService]
})
export class SpeficLigneArchiveesComponent implements OnInit, OnDestroy {

  @ViewChild('dt') dt: Table | undefined;
  private readonly destroy$ = new Subject<void>();

  // ========== REACTIVE STREAMS ==========
  private readonly selectedBanqueId$ = new BehaviorSubject<number | null>(null);
  private readonly marcheFilter$ = new BehaviorSubject<string>('');

  // ========== DATA TABLE ==========
  lignesFonctionnellesSpecifiques: LigneFonctionnelleSpecifiqueResponse[] = [];
  totalRecords = 0;
  loading = false;
  first = 0;
  rows = 10;
  sortField = 'id';
  sortOrder = 1;

  // ========== FORMS & OPTIONS ==========
  searchForm!: FormGroup;
  banquesOptions: any[] = [];
  natureLigneOptions: any[] = [];

  // ========== MARCHÉ DROPDOWN ==========
  marchesOptions: any[] = [];
  marchesLoading = false;
  marchesScrollerOptions: ScrollerOptions;

  items: MenuItem[] = [];

  constructor(
    private specificLigneService: SpecificFunctionalLigneService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private readonly lineNavigationService: LineNavigationService

  ) {
    this.marchesScrollerOptions = {
      delay: 250,
      showLoader: true,
      lazy: true,
      onLazyLoad: this.onLazyLoadMarches.bind(this)
    };
  }

  ngOnInit(): void {
    this.initBreadcrumb();
    this.initSearchForm();
    this.loadFilterOptions();
    this.setupReactiveStreams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initBreadcrumb(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', label: "Lignes Spécifiques Archivées" }
    ];
  }

  private initSearchForm(): void {
    this.searchForm = this.fb.group({
      banqueId: [null],
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
      actif: [false] // Toujours false pour les archives
    });
  }

  private setupReactiveStreams(): void {
    // 1. Recherche globale sur changement de formulaire
    this.searchForm.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(500),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => this.onSearch());

    // 2. Lien Banque -> Marché
    this.searchForm.get('banqueId')!.valueChanges.pipe(
      takeUntil(this.destroy$),
      tap(id => this.selectedBanqueId$.next(id))
    ).subscribe();

    // 3. Filtrage dynamique du dropdown Marché
    this.marcheFilter$.pipe(
      takeUntil(this.destroy$),
      debounceTime(DEBOUNCE_TIME),
      distinctUntilChanged(),
      switchMap(filter => this.loadMarchesData(0, filter, this.selectedBanqueId$.value))
    ).subscribe(res => this.initializeMarchesOptions(res));

    // Initialisation
    this.loadMarchesData(0).subscribe(res => this.initializeMarchesOptions(res));
  }

  // ========== MARCHÉS DROPDOWN LOGIC ==========

  private loadMarchesData(page: number, filter: string = '', banqueId?: number | null) {
    const criteria: any = {};
    if (filter) criteria.numero = filter;
    if (banqueId) criteria.banqueId = banqueId;

    return this.marcheService.getAllMarches(criteria, page, MARCHE_PAGE_SIZE, 'numero,asc').pipe(
      catchError(() => of({ content: [], page: { totalElements: 0 } }))
    );
  }

  private initializeMarchesOptions(response: any): void {
    const total = response.page.totalElements;
    const data = [{ label: 'Tous les marchés', value: null }];
    response.content.forEach((m: any) => data.push({ label: m.numero, value: m.id }));

    this.marchesOptions = Array.from({ length: total + 1 }).map(() => ({}));
    this.marchesOptions.splice(0, data.length, ...data);
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    if (this.marchesOptions[first]?.value !== undefined) return;

    this.marchesLoading = true;
    const page = Math.floor(first / MARCHE_PAGE_SIZE);

    this.loadMarchesData(page, this.marcheFilter$.value, this.selectedBanqueId$.value)
      .subscribe(res => {
        const newItems = res.content.map((m: any) => ({ label: m.numero, value: m.id }));
        const updated = [...this.marchesOptions];
        updated.splice(first, newItems.length, ...newItems);
        this.marchesOptions = updated;
        this.marchesLoading = false;
      });
  }

  onFilterMarches(event: any): void {
    this.marcheFilter$.next(event.filter?.trim() || '');
  }

  // ========== FILTERS LOAD ==========

  loadFilterOptions(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.banquesOptions = [{ label: 'Toutes les banques', value: null }, ...res.content.map(b => ({ label: b.nom, value: b.id }))];
    });

    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.natureLigneOptions = [{ label: 'Toutes les natures', value: null }, ...res.map(n => ({ label: n.label, value: n.id }))];
    });
  }

  // ========== MAIN TABLE LOGIC ==========

  loadLignesFonctionnellesSpecifiques(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.sortField = (Array.isArray(event.sortField) ? event.sortField[0] : event.sortField) || 'id';
    this.sortOrder = event.sortOrder ?? 1;

    const sort = `${this.sortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`;
    const searchCriteria: LigneFonctionnelleSpecifiqueSearchCriteria = this.searchForm.value;

    this.specificLigneService.getLignesFonctionnellesSpecifiques(searchCriteria, this.first / this.rows, this.rows, sort)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.lignesFonctionnellesSpecifiques = data.content;
          this.totalRecords = data.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    }
  }

  clearSearch(): void {
    this.searchForm.reset({ banqueId: null, natureLigneFonctionnelleId: null, marcheId: null, actif: false });
  }

  redirectToLineDetails(lineId: number, label: string): void {
    // Pour les lignes GÉNÉRALES, on utilise LineType.GENERAL
    this.lineNavigationService.redirectToLineDetails(lineId, label, LineType.SPECIFIC);
  }}
