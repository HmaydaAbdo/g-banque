  import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
  import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
  import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
  import { RouterLink, RouterLinkActive } from '@angular/router';
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
  import { Role } from "../../../../auth/enum/Role";
  import { AuthService } from "../../../../auth/services/auth.service";
  import { LigneFonctionnelleSpecifiqueResponse } from "../../dtos/LigneFonctionnelleSpecifiqueResponse";
  import { LigneFonctionnelleSpecifiqueSearchCriteria } from "../../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
  import { SpecificFunctionalLigneService } from "../../service/SpecificFunctionalLigneService";
  import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
  import { MarcheService } from "../../../Marches/service/marche-service.service";
  import { NatureLigneFonctionnelleService } from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";

  const MARCHE_PAGE_SIZE = 10;
  const DEBOUNCE_TIME = 300;

  @Component({
    selector: 'app-spefic-ligne-encours',
    templateUrl: './spefic-ligne-encours.component.html',
    styleUrls: ['./spefic-ligne-encours.component.scss'],
    standalone: true,
    imports: [
      CommonModule, ReactiveFormsModule, FormsModule, DecimalPipe, DatePipe,
      RouterLink, RouterLinkActive, TableModule, InputTextModule, ButtonModule,
      RippleModule, ToastModule, ConfirmDialogModule, ToolbarModule, TagModule,
      TooltipModule, DropdownModule, InputNumberModule, CalendarModule,
      InputSwitchModule, BreadcrumbModule
    ],
    providers: [MessageService, ConfirmationService]
  })
  export class SpeficLigneEncoursComponent implements OnInit, OnDestroy {

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
    sortOrder = -1;

    // ========== FORMS & OPTIONS ==========
    searchForm!: FormGroup;
    banquesOptions: any[] = [];
    natureLigneOptions: any[] = [];
    statutOptions = [
      { label: 'Tous', value: null },
      { label: 'En cours', value: false },
      { label: 'Expirées', value: true }
    ];

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
      private confirmationService: ConfirmationService,
      private authService: AuthService
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
        { icon: 'pi pi-list', label: "Lignes Spécifiques" }
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

    private setupReactiveStreams(): void {
      // 1. Détection des changements dans le formulaire (recherche globale)
      this.searchForm.valueChanges.pipe(
        takeUntil(this.destroy$),
        debounceTime(500),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      ).subscribe(() => this.onSearch());

      // 2. Gestion de la dépendance Banque -> Marché
      this.searchForm.get('banqueId')!.valueChanges.pipe(
        takeUntil(this.destroy$),
        tap(id => this.selectedBanqueId$.next(id))
      ).subscribe();

      // 3. Gestion du filtre texte du dropdown Marché
      this.marcheFilter$.pipe(
        takeUntil(this.destroy$),
        debounceTime(DEBOUNCE_TIME),
        distinctUntilChanged(),
        switchMap(filter => this.loadMarchesData(0, filter, this.selectedBanqueId$.value))
      ).subscribe(res => this.initializeMarchesOptions(res));

      // Initialisation initiale des marchés
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

      // On prépare le tableau pour le Virtual Scroll (placeholders)
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
      this.sortOrder = event.sortOrder ?? -1;

      const sort = `${this.sortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`;
      const criteria: LigneFonctionnelleSpecifiqueSearchCriteria = this.searchForm.value;

      this.specificLigneService.getLignesFonctionnellesSpecifiques(criteria, this.first / this.rows, this.rows, sort)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (data) => {
            this.lignesFonctionnellesSpecifiques = data.content;
            this.totalRecords = data.page.totalElements;
            this.loading = false;
          },
          error: () => {
            this.loading = false;
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement.' });
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
      this.searchForm.reset({ banqueId: null, natureLigneFonctionnelleId: null, marcheId: null, actif: true, isExpired: null });
    }

    // ========== UI HELPERS ==========

    getSoldeClass(solde: number | null): string {
      if (solde === null || solde === undefined) return '';
      return solde < 0 ? 'text-red-600 font-bold' : 'text-green-600 font-bold';
    }

    isAdmin(): boolean {
      return this.authService.hasRole(Role.ADMIN);
    }

    private reloadTableAfterAction(): void {
      if (this.dt) this.dt.onLazyLoad.emit(this.dt.createLazyLoadMetadata());
    }

    // ========== ACTIONS ==========

    desactiver(ligne: LigneFonctionnelleSpecifiqueResponse): void {
      this.confirmationService.confirm({
        message: `Désactiver la ligne ${ligne.natureLigneFonctionnelleLabel} ?`,
        header: 'Confirmation',
        icon: 'pi pi-exclamation-triangle',
        accept: () => {
          this.specificLigneService.deactivateLigneFonctionnelle(ligne.id!).subscribe(() => {
            this.messageService.add({ severity: 'success', summary: 'Désactivée' });
            this.reloadTableAfterAction();
          });
        }
      });
    }

    deleteLigneFonctionnelleSpecifique(ligne: LigneFonctionnelleSpecifiqueResponse): void {
      this.confirmationService.confirm({
        message: `Supprimer définitivement cette ligne ?`,
        header: 'Attention',
        icon: 'pi pi-trash',
        accept: () => {
          this.specificLigneService.deleteLigneFonctionnelleSpecifique(ligne.id!).subscribe(() => {
            this.messageService.add({ severity: 'success', summary: 'Supprimée' });
            this.reloadTableAfterAction();
          });
        }
      });
    }
  }
