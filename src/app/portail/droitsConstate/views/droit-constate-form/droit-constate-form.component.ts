import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// PrimeNG
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';

// Services & DTOs
import { DroitConstateService } from "../../service/DroitConstateService";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { Type } from "../../../prefinancements/enums/Type";
import { DroitConstateRequestDto } from '../../dtos/DroitConstateRequestDto';

@Component({
  selector: 'app-droit-constate-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, InputTextModule, DropdownModule,
    InputNumberModule, ButtonModule, RippleModule, ToastModule, ConfirmDialogModule,
    CalendarModule, BreadcrumbModule, RouterLink, RouterLinkActive, TooltipModule,
  ],
  templateUrl: './droit-constate-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class DroitConstateFormComponent implements OnInit {
  droitConstateForm!: FormGroup;
  isEditMode = false;
  droitConstateId: number | null = null;
  loading = false;

  // Configuration Marchés
  marchesOptions: any[] = [];
  loadingMarches = false;
  currentFilter = '';
  rowsPerPage = 10;
  private filterSubject = new Subject<string>();

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  typeOptions: any[] = [];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private droitConstateService: DroitConstateService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.setupBreadcrumbs();
    this.initForm();
    this.loadTypeOptions();

    // Initialisation de la recherche avec debounce
    this.filterSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(query => {
      this.loadInitialMarches(query);
    });

    // Chargement initial (page 0)
    this.loadInitialMarches();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.droitConstateId = Number(id);
      this.loadDroitConstate(this.droitConstateId);
    }
  }

  initForm(): void {
    this.droitConstateForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
      type: [null, Validators.required],
    });
  }

  /** Chargement initial pour une requête donnée (filtre ou vide) */
  loadInitialMarches(query: string = ''): void {
    this.loadingMarches = true;
    this.currentFilter = query;
    this.marcheService.getAllMarches({ numero: query }, 0, this.rowsPerPage, 'numero,asc').subscribe({
      next: (res) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        // Création du tableau "placeholder" pour le scroll virtuel
        this.marchesOptions = Array.from({ length: total }).map(() => ({}));
        this.marchesOptions.splice(0, data.length, ...data);
        this.marchesOptions = [...this.marchesOptions]; // Trigger change detection
        this.loadingMarches = false;
      },
      error: () => this.loadingMarches = false
    });
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / this.rowsPerPage);

    // Si la donnée est déjà présente, on ne recharge pas
    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) {
      return;
    }

    this.loadingMarches = true;
    this.marcheService.getAllMarches({ numero: this.currentFilter }, page, this.rowsPerPage, 'numero,asc').subscribe({
      next: (res) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        this.marchesOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = [...this.marchesOptions]; // Important pour le rafraîchissement
        this.loadingMarches = false;
      },
      error: () => this.loadingMarches = false
    });
  }

  onFilterMarches(event: any): void {
    this.filterSubject.next(event.filter?.trim() || '');
  }

  loadDroitConstate(id: number): void {
    this.loading = true;
    this.droitConstateService.getDroitConstateById(id).subscribe({
      next: (dto) => {
        // Pour s'assurer que le marché actuel est dans la liste du dropdown
        this.marcheService.getMarcheById(dto.marcheId).subscribe(m => {
          const selected = { label: m.numero, value: m.id };
          if (!this.marchesOptions.some(opt => opt.value === m.id)) {
            this.marchesOptions[0] = selected; // On l'injecte temporairement
            this.marchesOptions = [...this.marchesOptions];
          }
          this.droitConstateForm.patchValue({
            marcheId: dto.marcheId,
            dateEmission: new Date(dto.dateEmission),
            montant: dto.montant,
            type: dto.type,
          });
        });
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement.' });
        this.router.navigate(['/portail/droits-constate']);
      }
    });
  }

  loadTypeOptions(): void {
    this.typeOptions = Object.keys(Type).map((key) => ({
      label: key,
      value: Type[key as keyof typeof Type],
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', routerLink: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', routerLink: '/portail/droits-constate', label: 'Droits constatés' },
    ];
  }

  get f() { return this.droitConstateForm.controls; }

  onSubmit(): void {
    if (this.droitConstateForm.invalid) {
      this.droitConstateForm.markAllAsTouched();
      return;
    }
    this.confirmationService.confirm({
      message: this.isEditMode ? 'Modifier ce droit constaté ?' : 'Créer ce droit constaté ?',
      header: 'Confirmation',
      accept: () => this.executeSave()
    });
  }

  executeSave(): void {
    this.loading = true;
    const request: DroitConstateRequestDto = this.droitConstateForm.value;

    const op$ = (this.isEditMode && this.droitConstateId)
      ? this.droitConstateService.updateDroitConstate(this.droitConstateId, request)
      : this.droitConstateService.createDroitConstate(request);

    op$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Opération réussie.' });
        this.router.navigate(['/portail/droits-constate']);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message || 'Erreur technique' });
        this.loading = false;
      }
    });
  }

  onCancel(): void { this.router.navigate(['/portail/droits-constate']); }
}
