import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf,Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';

// PrimeNG Imports
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
import { PrefinancementService } from "../../service/PrefinancementService";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { Type } from '../../enums/Type';
import { PrefinancementRequestDTO } from '../../dtos/PrefinancementRequestDTO';
import { PrefinancementResponseDTO } from '../../dtos/PrefinancementResponseDTO';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-prefinancement-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NgIf,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    CalendarModule,
    BreadcrumbModule,
    RouterLink,
    RouterLinkActive,
    TooltipModule,
  ],
  templateUrl: './prefinancement-form.component.html',
  styleUrl: './prefinancement-form.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class PrefinancementFormComponent implements OnInit {
  prefinancementForm!: FormGroup;
  isEditMode: boolean = false;
  prefinancementId: number | null = null;
  loading: boolean = false; // Loading global pour le bouton submit

  // Configuration pour le Dropdown Marchés (Lazy & Virtual Scroll)
  marchesOptions: any[] = [];
  loadingMarches: boolean = false;
  currentFilter: string = '';
  rowsPerPage: number = 10;

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  banquesOptions: any[] = [];
  typeOptions: any[] = [];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private prefinancementService: PrefinancementService,
    private marcheService: MarcheService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.setupBreadcrumbs();
    this.initForm();
    this.loadStaticDropdownData();
    this.initializeMarches(); // Initialisation de la liste des marchés

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.prefinancementId = Number(id);
        this.loadPrefinancement(this.prefinancementId);
      }
    });
  }

  initForm(): void {
    this.prefinancementForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
      type: [null, Validators.required],
    });
  }

  // ✅ INITIALISATION : Charge le total et évite l'erreur 'optionGroup'
  initializeMarches(): void {
    this.marcheService.getAllMarchesForLazyLoad(0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));

      // On remplit avec des objets vides {} pour satisfaire le moteur ARIA de PrimeNG
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));

      // On injecte les premières données
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  // ✅ LAZY LOAD : Appelé lors du scroll du dropdown
  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / this.rowsPerPage);

    // Si la donnée existe déjà (value définie), on ne recharge pas
    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) {
      return;
    }

    this.loadingMarches = true;
    this.marcheService.getAllMarches({ numero: this.currentFilter }, page, this.rowsPerPage, 'numero,asc').subscribe({
      next: (res) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        const updatedOptions = [...this.marchesOptions];
        updatedOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = updatedOptions;
        this.loadingMarches = false;
      },
      error: () => this.loadingMarches = false
    });
  }

  // ✅ FILTER : Appelé lors de la saisie dans le champ recherche
  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentFilter = query;

    this.marcheService.getAllMarches({ numero: query }, 0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));

      // Recréation du tableau selon la taille des résultats filtrés
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  loadStaticDropdownData(): void {
    // Type Enum
    this.typeOptions = Object.keys(Type).map((key) => ({
      label: key,
      value: Type[key as keyof typeof Type],
    }));

    // Banques (Chargement simple car souvent moins volumineux)
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (res) => {
        this.banquesOptions = res.content.map((banque) => ({
          label: banque.nom,
          value: banque.id,
        }));
      }
    });
  }

  loadPrefinancement(id: number): void {
    this.loading = true;
    this.prefinancementService.getPrefinancementById(id).subscribe({
      next: (prefinancement: PrefinancementResponseDTO) => {

        // Charger spécifiquement le marché sélectionné pour l'afficher en mode édition
        this.marcheService.getMarcheById(prefinancement.marcheId).subscribe(marche => {
          const selected = { label: marche.numero, value: marche.id };

          // L'injecter au début de la liste s'il n'y est pas
          if (!this.marchesOptions.find(m => m?.value === marche.id)) {
            this.marchesOptions[0] = selected;
          }

          this.prefinancementForm.patchValue({
            marcheId: prefinancement.marcheId,
            dateEmission: parseLocalDate(prefinancement.dateEmission),
            montant: prefinancement.montant,
            type: prefinancement.type,
          });
        });

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement.' });
        this.location.back();
      }
    });
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/préfinancements', label: 'Préfinancements' },
    ];
  }

  get f() { return this.prefinancementForm.controls; }

  onSubmit(): void {
    if (this.prefinancementForm.invalid) {
      this.prefinancementForm.markAllAsTouched();
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Modifier ce préfinancement ?' : 'Créer ce préfinancement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave()
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.prefinancementForm.value;
    const request: PrefinancementRequestDTO = {
      ...formValue,
      dateEmission: toLocalDateString(formValue.dateEmission)!
    };

    const operation$ = (this.isEditMode && this.prefinancementId)
      ? this.prefinancementService.updatePrefinancement(this.prefinancementId, request)
      : this.prefinancementService.createPrefinancement(request);

    operation$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Enregistré avec succès.' });
        this.location.back();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message || 'Erreur technique' });
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.location.back();
  }
}
