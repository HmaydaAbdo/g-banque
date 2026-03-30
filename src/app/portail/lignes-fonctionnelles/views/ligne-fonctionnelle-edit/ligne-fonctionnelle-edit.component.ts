import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidatorFn,
  FormArray
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';

// RxJS
import {
  Subject,
  BehaviorSubject,
  combineLatest,
  of,
  EMPTY
} from 'rxjs';
import {
  takeUntil,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  tap,
  catchError,
  filter,
  map
} from 'rxjs/operators';

// PrimeNG
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { BreadcrumbModule } from "primeng/breadcrumb";
import { TooltipModule } from "primeng/tooltip";

// Services & DTOs
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { LigneFonctionnelleRequest } from "../../dtos/LigneFonctionnelleRequest";
import { LigneFonctionnelleResponse } from '../../dtos/LigneFonctionnelleResponse';
import { NatureLigneFonctionnelleService } from "../../services/nature-ligne-fonctionnelle.service";
import { PorteeNature } from "../../dtos/nature-ligne-fonctionnelle/PorteeNature";
import { SousLigneResponseDto } from "../../dtos/SousLigneResponseDto";
import { SousLigneRequestDto } from "../../dtos/SousLigneRequestDto";
import { MarcheService } from "../../../Marches/service/marche-service.service";
import { parseLocalDate, toLocalDateString } from "../../../global/types/date.utils";

// ========== CONSTANTS ==========
const CAUTION_ADMINISTRATIVE_ID = 16;
const MARCHE_PAGE_SIZE = 10;
const DEBOUNCE_TIME = 300;

@Component({
  selector: 'app-ligne-fonctionnelle-edit',
  templateUrl: './ligne-fonctionnelle-edit.component.html',
  styleUrls: ['./ligne-fonctionnelle-edit.component.scss'],
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, NgIf, InputTextModule, DropdownModule,
    InputNumberModule, ButtonModule, RippleModule, ToastModule, ConfirmDialogModule,
    CalendarModule, BreadcrumbModule, RouterLink, RouterLinkActive, TooltipModule,
    DialogModule, TableModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class LigneFonctionnelleEditComponent implements OnInit, OnDestroy {

  // ========== REACTIVE STREAMS ==========
  private readonly destroy$ = new Subject<void>();
  private readonly selectedBanqueId$ = new BehaviorSubject<number | null>(null);
  private readonly marcheFilter$ = new BehaviorSubject<string>('');

  // ========== FORM GROUPS ==========
  ligneFonctionnelleForm!: FormGroup;
  sousLigneForm!: FormGroup;

  // ========== STATE ==========
  isEditMode = false;
  ligneFonctionnelleId: number | null = null;
  loading = false;

  // ========== UI DATA ==========
  totalSousLignesPlafond = 0;
  natureLigneOptions: Array<{ label: string; value: number }> = [];
  banquesOptions: Array<{ label: string; value: number }> = [];
  marchesOptions: any[] = [];
  items: MenuItem[] = [];

  // ========== DIALOG STATE ==========
  displaySousLigneDialog = false;
  sousLigneEditMode = false;
  currentSousLigneIndex: number | null = null;

  // ========== MARCHÉ LAZY LOADING ==========
  loadingMarches = false;
  marchesScrollerOptions: ScrollerOptions;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly ligneFonctionnelleService: LigneFonctionnelleService,
    private readonly banqueService: BanqueService,
    private readonly natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private readonly messageService: MessageService,
    private readonly confirmationService: ConfirmationService,
    private readonly marcheService: MarcheService
  ) {
    this.marchesScrollerOptions = {
      delay: 250,
      showLoader: true,
      lazy: true,
      onLazyLoad: this.onLazyLoadMarches.bind(this)
    };
  }

  // ========== LIFECYCLE HOOKS ==========

  ngOnInit(): void {
    this.initBreadcrumb();
    this.initForm();
    this.loadStaticDropdowns();
    this.setupReactiveStreams();
    this.handleRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ========== INITIALIZATION ==========

  private initBreadcrumb(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label: "Lignes fonctionnelles" }
    ];
  }

  private initForm(): void {
    this.ligneFonctionnelleForm = this.fb.group({
      banqueId: [null, Validators.required],
      natureLigneFonctionnelleId: [null, Validators.required],
      marcheId: [null], // ✅ Optionnel
      plafond: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      taux: [null, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      dateDepart: [null, Validators.required],
      dateFin: [null, Validators.required],
      sousLignes: this.fb.array([], [this.sousLigneNatureUniquenessValidator()])
    }, {
      validators: [this.dateFinAfterDateDepartValidator()]
    });

    this.sousLigneForm = this.fb.group({
      id: [null],
      natureLigneFonctionnelleId: [null, Validators.required],
      plafond: [null, [Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]]
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
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des données de référence',
          life: 3000
        });
      }
    });
  }

  private loadBanques() {
    return this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').pipe(
      map(response => response.content.map(banque => ({
        label: banque.nom,
        value: banque.id!
      }))),
      catchError(err => {
        console.error('Error loading banques:', err);
        return of([]);
      })
    );
  }

  private loadNatureLignes() {
    return this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList(PorteeNature.GENERALE).pipe(
      map(natures => natures.map(nature => ({
        label: nature.label,
        value: nature.id!
      }))),
      catchError(err => {
        console.error('Error loading natures:', err);
        return of([]);
      })
    );
  }

  // ========== REACTIVE STREAMS SETUP ==========

  private setupReactiveStreams(): void {
    this.setupBanqueChangeStream();
    this.setupMarcheFilterStream();
    this.setupSousLignesStream();
    this.setupNatureChangeStream();
  }

  private setupBanqueChangeStream(): void {
    this.ligneFonctionnelleForm.get('banqueId')!.valueChanges.pipe(
      takeUntil(this.destroy$),
      tap(banqueId => {
        // Reset marché quand banque change
        this.ligneFonctionnelleForm.patchValue({ marcheId: null }, { emitEvent: false });
        this.selectedBanqueId$.next(banqueId);
        this.marchesOptions = []; // Clear previous options
      }),
      filter(banqueId => !!banqueId),
      switchMap(banqueId => this.loadMarchesForBanque(banqueId, 0))
    ).subscribe({
      next: (response) => {
        this.initializeMarchesOptions(response);
      },
      error: (err) => {
        console.error('Error loading marchés:', err);
        this.marchesOptions = [];
      }
    });
  }

  private setupMarcheFilterStream(): void {
    combineLatest([
      this.marcheFilter$.pipe(debounceTime(DEBOUNCE_TIME), distinctUntilChanged()),
      this.selectedBanqueId$.pipe(filter(id => id !== null))
    ]).pipe(
      takeUntil(this.destroy$),
      switchMap(([filter, banqueId]) =>
        this.loadMarchesForBanque(banqueId!, 0, filter)
      )
    ).subscribe({
      next: (response) => {
        this.initializeMarchesOptions(response);
      },
      error: (err) => {
        console.error('Error filtering marchés:', err);
      }
    });
  }

  private setupSousLignesStream(): void {
    this.sousLignesArray.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(100)
    ).subscribe(() => {
      this.calculateTotalSousLignesPlafond();
      this.ligneFonctionnelleForm.updateValueAndValidity();
    });

    this.ligneFonctionnelleForm.get('plafond')!.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(100)
    ).subscribe(() => {
      this.ligneFonctionnelleForm.updateValueAndValidity();
    });
  }

  private setupNatureChangeStream(): void {
    this.ligneFonctionnelleForm.get('natureLigneFonctionnelleId')!.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(natureId => {
      if (natureId !== CAUTION_ADMINISTRATIVE_ID) {
        this.sousLignesArray.clear();
      }
      this.ligneFonctionnelleForm.updateValueAndValidity();
    });
  }

  // ========== MARCHÉ LOADING ==========

  private loadMarchesForBanque(banqueId: number, page: number, filter: string = '') {
    return this.marcheService.getAllMarches(
      { banqueId, numero: filter },
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
    const data = response.content.map((m: any) => ({ label: m.numero, value: m.id }));

    this.marchesOptions = Array.from({ length: total }).map(() => ({}));
    this.marchesOptions.splice(0, data.length, ...data);
  }

  onLazyLoadMarches(event: any): void {
    const banqueId = this.selectedBanqueId$.value;
    if (!banqueId) return;

    const { first } = event;
    const page = Math.floor(first / MARCHE_PAGE_SIZE);

    if (this.marchesOptions[first]?.value !== undefined) {
      return;
    }

    this.loadingMarches = true;
    this.loadMarchesForBanque(banqueId, page, this.marcheFilter$.value)
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

  // ========== ROUTE HANDLING ==========

  private handleRouteParams(): void {
    this.route.paramMap.pipe(
      takeUntil(this.destroy$),
      map(params => params.get('id')),
      tap(id => {
        this.isEditMode = !!id;
        this.ligneFonctionnelleId = id ? Number(id) : null;
      }),
      filter(id => !!id),
      switchMap(id => this.loadLigneFonctionnelle(Number(id)))
    ).subscribe();
  }

  private loadLigneFonctionnelle(id: number) {
    this.loading = true;

    return this.ligneFonctionnelleService.getLigneFonctionnelleById(id).pipe(
      takeUntil(this.destroy$),
      tap(ligne => this.patchFormWithLigne(ligne)),
      catchError(error => {
        console.error('Error loading ligne:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.error?.message || 'Impossible de charger la ligne',
          life: 3000
        });
        this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
        return EMPTY;
      }),
      tap(() => this.loading = false)
    );
  }

  private patchFormWithLigne(ligne: LigneFonctionnelleResponse): void {
    // Patch banque first pour trigger marché loading
    this.ligneFonctionnelleForm.patchValue({
      banqueId: ligne.banqueId
    }, { emitEvent: true });

    // Attendre que les marchés soient chargés
    setTimeout(() => {
      if (ligne.marcheId) {
        this.ensureMarcheInOptions(ligne.marcheId).pipe(
          takeUntil(this.destroy$)
        ).subscribe(() => {
          this.patchRemainingFields(ligne);
        });
      } else {
        this.patchRemainingFields(ligne);
      }
    }, 500);
  }

  private ensureMarcheInOptions(marcheId: number) {
    if (this.marchesOptions.find(m => m?.value === marcheId)) {
      return of(null);
    }

    return this.marcheService.getMarcheById(marcheId).pipe(
      tap(marche => {
        const selected = { label: marche.numero, value: marche.id };
        this.marchesOptions[0] = selected;
      }),
      catchError(() => of(null))
    );
  }

  private patchRemainingFields(ligne: LigneFonctionnelleResponse): void {
    this.ligneFonctionnelleForm.patchValue({
      marcheId: ligne.marcheId,
      natureLigneFonctionnelleId: ligne.natureLigneFonctionnelleId,
      plafond: ligne.plafond,
      taux: ligne.taux,
      dateDepart: parseLocalDate(ligne.dateDepart),
      dateFin: parseLocalDate(ligne.dateFin)
    }, { emitEvent: false });

    if (ligne.estLigneMere && ligne.sousLignes?.length) {
      this.sousLignesArray.clear();
      ligne.sousLignes.forEach(sl => {
        this.sousLignesArray.push(this.createSousLigneFormGroup(sl));
      });
      this.calculateTotalSousLignesPlafond();
    }
  }

  // ========== SOUS-LIGNES MANAGEMENT ==========

  openAddSousLigneDialog(): void {
    this.sousLigneEditMode = false;
    this.currentSousLigneIndex = null;
    this.sousLigneForm.reset();
    this.displaySousLigneDialog = true;
  }

  openEditSousLigneDialog(index: number): void {
    this.sousLigneEditMode = true;
    this.currentSousLigneIndex = index;
    this.sousLigneForm.patchValue(this.sousLignesArray.at(index).value);
    this.displaySousLigneDialog = true;
  }

  onSousLigneDialogHide(): void {
    this.sousLigneForm.reset();
    this.sousLigneEditMode = false;
    this.currentSousLigneIndex = null;
  }

  saveSousLigne(): void {
    this.sousLigneForm.markAllAsTouched();

    if (this.sousLigneForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs',
        life: 3000
      });
      return;
    }

    const sousLigneValue = this.sousLigneForm.value;

    if (this.sousLigneEditMode && this.currentSousLigneIndex !== null) {
      this.sousLignesArray.at(this.currentSousLigneIndex).patchValue(sousLigneValue);
      this.messageService.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Sous-ligne modifiée',
        life: 1500
      });
    } else {
      this.sousLignesArray.push(this.createSousLigneFormGroup(sousLigneValue));
      this.messageService.add({
        severity: 'success',
        summary: 'Succès',
        detail: 'Sous-ligne ajoutée',
        life: 1500
      });
    }

    if (this.sousLignesArray.hasError('duplicateSousLigneNature')) {
      this.messageService.add({
        severity: 'error',
        summary: 'Doublon',
        detail: 'Cette nature existe déjà',
        life: 5000
      });
    }

    this.displaySousLigneDialog = false;
  }

  removeSousLigne(index: number): void {
    this.sousLignesArray.removeAt(index);
  }

  private createSousLigneFormGroup(sousLigne?: SousLigneResponseDto | SousLigneRequestDto): FormGroup {
    const id = (sousLigne && 'id' in sousLigne) ? sousLigne.id : null;

    return this.fb.group({
      id: [id],
      plafond: [
        sousLigne?.plafond ?? null,
        [Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]
      ],
      natureLigneFonctionnelleId: [
        sousLigne?.natureLigneFonctionnelleId ?? null,
        Validators.required
      ]
    });
  }

  private calculateTotalSousLignesPlafond(): void {
    this.totalSousLignesPlafond = this.sousLignesArray.controls.reduce((sum, control) => {
      const plafond = control.get('plafond')?.value;
      return sum + (typeof plafond === 'number' && plafond > 0 ? plafond : 0);
    }, 0);
  }

  // ========== FORM SUBMISSION ==========

  onSubmit(): void {
    this.ligneFonctionnelleForm.markAllAsTouched();
    this.ligneFonctionnelleForm.updateValueAndValidity();

    if (this.ligneFonctionnelleForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire',
        life: 3000
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode
        ? 'Êtes-vous sûr de vouloir modifier cette ligne ?'
        : 'Êtes-vous sûr de vouloir créer cette ligne ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
      reject: () => {
        this.messageService.add({
          severity: 'info',
          summary: 'Annulé',
          detail: 'Opération annulée',
          life: 2000
        });
      }
    });
  }

  private executeSave(): void {
    this.loading = true;

    const request = this.buildRequest();

    const save$ = this.isEditMode && this.ligneFonctionnelleId
      ? this.ligneFonctionnelleService.updateLigneFonctionnelle(this.ligneFonctionnelleId, request)
      : this.ligneFonctionnelleService.createLigneFonctionnelle(request);

    save$.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Ligne ${this.isEditMode ? 'modifiée' : 'créée'} avec succès`,
          life: 3000
        });
        this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
      },
      error: (error) => {
        console.error('Save error:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.error?.message || 'Échec de l\'enregistrement',
          life: 3000
        });
        this.loading = false;
      }
    });
  }

  private buildRequest(): LigneFonctionnelleRequest {
    const formValue = this.ligneFonctionnelleForm.value;

    const sousLignes = this.isCautionAdministrativeSelected && formValue.sousLignes.length > 0
      ? formValue.sousLignes
      : [];

    return {
      banqueId: formValue.banqueId,
      natureLigneFonctionnelleId: formValue.natureLigneFonctionnelleId,
      marcheId: formValue.marcheId || null, // ✅ Peut être null
      plafond: formValue.plafond,
      taux: formValue.taux,
      dateDepart: toLocalDateString(formValue.dateDepart)!,
      dateFin: toLocalDateString(formValue.dateFin)!,
      sousLignes: this.isEditMode ? [] : sousLignes
    };
  }

  onCancel(): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
  }

  // ========== VALIDATORS ==========

  private dateFinAfterDateDepartValidator(): ValidatorFn {
    return (group: AbstractControl): { [key: string]: any } | null => {
      const depart = group.get('dateDepart');
      const fin = group.get('dateFin');

      if (!depart?.value || !fin?.value) {
        return null;
      }

      if (new Date(fin.value) < new Date(depart.value)) {
        fin.setErrors({ ...fin.errors, minDate: true });
      } else if (fin.hasError('minDate')) {
        const errors = { ...fin.errors };
        delete errors['minDate'];
        fin.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }

      return null;
    };
  }

  private sousLigneNatureUniquenessValidator(): ValidatorFn {
    return (formArray: AbstractControl): { [key: string]: any } | null => {
      if (!(formArray instanceof FormArray)) {
        return null;
      }

      const natures: number[] = [];
      let hasDuplicate = false;

      for (const control of formArray.controls) {
        const natureControl = control.get('natureLigneFonctionnelleId');
        const natureId = natureControl?.value;

        // Reset duplicate error
        if (natureControl?.hasError('duplicateNature')) {
          const errors = { ...natureControl.errors };
          delete errors['duplicateNature'];
          natureControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }

        if (natureId != null) {
          if (natures.includes(natureId)) {
            hasDuplicate = true;
            natureControl?.setErrors({
              ...natureControl.errors,
              duplicateNature: true
            });
          } else {
            natures.push(natureId);
          }
        }
      }

      return hasDuplicate ? { duplicateSousLigneNature: true } : null;
    };
  }

  // ========== GETTERS & HELPERS ==========

  get sousLignesArray(): FormArray {
    return this.ligneFonctionnelleForm.get('sousLignes') as FormArray;
  }

  get isCautionAdministrativeSelected(): boolean {
    return this.ligneFonctionnelleForm.get('natureLigneFonctionnelleId')?.value === CAUTION_ADMINISTRATIVE_ID;
  }

  get f() {
    return this.ligneFonctionnelleForm.controls;
  }

  get selectedBanqueId(): number | null {
    return this.selectedBanqueId$.value;
  }

  getNatureLabel(id: number | null): string {
    return this.natureLigneOptions.find(n => n.value === id)?.label || 'Inconnue';
  }
}
