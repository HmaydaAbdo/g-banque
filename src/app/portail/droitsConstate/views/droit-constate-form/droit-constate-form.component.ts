import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { Type } from "../../../prefinancements/enums/Type";
import { DroitConstateRequestDto } from '../../dtos/DroitConstateRequestDto';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-droit-constate-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, InputTextModule, DropdownModule,
    InputNumberModule, ButtonModule, RippleModule, ToastModule, ConfirmDialogModule,
    CalendarModule, BreadcrumbModule, RouterLink, TooltipModule,
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
  banquesOptions: { label: string; value: number }[] = [];
  loadingBanques = false;
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private droitConstateService: DroitConstateService,
    private marcheService: MarcheService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.setupBreadcrumbs();
    this.initForm();
    this.loadTypeOptions();
    this.loadBanques();

    // Initialisation de la recherche avec debounce
    this.filterSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(query => {
      this.loadInitialMarches(query);
    });

    // Chargement initial (page 0)
    this.loadInitialMarches();

    // Quand l'utilisateur change de marché, pré-remplir la banque automatiquement
    this.droitConstateForm.get('marcheId')!.valueChanges.subscribe(marcheId => {
      if (marcheId && !this.isEditMode) {
        this.prefillBanqueFromMarche(marcheId);
      }
    });

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
      banqueId: [null, Validators.required],
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
            banqueId: dto.banqueId,
            dateEmission: parseLocalDate(dto.dateEmission),
            montant: dto.montant,
            type: dto.type,
          });
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du droit constaté:', err);
        this.location.back();
      }
    });
  }

  loadTypeOptions(): void {
    this.typeOptions = [
      { label: 'Déblocage (encaissement)', value: Type.DEBLOCAGE },
      { label: 'Remboursement (décaissement)', value: Type.REMBOURSEMENT },
    ];
  }

  /** Charge toutes les banques pour le dropdown banque. */
  loadBanques(): void {
    this.loadingBanques = true;
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (res) => {
        this.banquesOptions = res.content.map(b => ({ label: b.nom, value: b.id }));
        this.loadingBanques = false;
      },
      error: () => { this.loadingBanques = false; }
    });
  }

  /**
   * Pré-remplit le champ banque depuis la banque du marché sélectionné.
   * L'utilisateur peut ensuite modifier ce choix librement.
   */
  prefillBanqueFromMarche(marcheId: number): void {
    this.marcheService.getMarcheById(marcheId).subscribe({
      next: (marche) => {
        if (marche.banqueId) {
          this.droitConstateForm.patchValue({ banqueId: marche.banqueId }, { emitEvent: false });
        }
      }
    });
  }

  /**
   * Libellé contextuel du champ banque selon le type sélectionné.
   * Aide l'utilisateur à comprendre le rôle de la banque dans l'opération.
   */
  get banqueLabel(): string {
    const type = this.droitConstateForm.get('type')?.value;
    if (type === Type.DEBLOCAGE) return 'Banque d\'encaissement (reçoit les fonds du Trésor) *';
    if (type === Type.REMBOURSEMENT) return 'Banque de décaissement (effectue le remboursement) *';
    return 'Banque *';
  }

  /**
   * Helper text contextuel selon le type sélectionné.
   */
  get banqueHelperText(): string {
    const type = this.droitConstateForm.get('type')?.value;
    if (type === Type.DEBLOCAGE)
      return 'Pré-remplie depuis la banque du marché. Modifiable si les fonds arrivent sur un autre compte.';
    if (type === Type.REMBOURSEMENT)
      return 'Pré-remplie depuis la banque du marché. Modifiable si le remboursement part d\'un autre compte.';
    return 'Sélectionnez d\'abord un marché pour pré-remplir automatiquement.';
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
    const formValue = this.droitConstateForm.value;
    const request: DroitConstateRequestDto = {
      ...formValue,
      dateEmission: toLocalDateString(formValue.dateEmission)!
    };

    const op$ = (this.isEditMode && this.droitConstateId)
      ? this.droitConstateService.updateDroitConstate(this.droitConstateId, request)
      : this.droitConstateService.createDroitConstate(request);

    op$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Opération réussie.' });
        this.location.back();
      },
      error: (err) => {
        console.error('Erreur lors de l\'enregistrement du droit constaté:', err);
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.location.back();
  }
}
