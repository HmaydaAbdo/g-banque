import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';
import { DecompteRequestDto } from '../../dtos/DecompteRequestDto';
import { DecompteResponseDto } from '../../dtos/DecompteResponseDto';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { PageResponse } from '../../../global/types/PageResponse';
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
import { MarcheResponseDTO } from '../../../Marches/dtos/marche-response.dto';
import { DecompteService } from '../../service/DecompteService';

@Component({
  selector: 'app-decompte-form',
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
  templateUrl: './decomptes-form.component.html',
  styleUrl: './decomptes-form.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class DecompteFormComponent implements OnInit {
  decompteForm!: FormGroup;
  isEditMode: boolean = false;
  decompteId: number | null = null;
  loading: boolean = false;

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

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private decompteService: DecompteService,
    private marcheService: MarcheService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupBreadcrumbs();
    this.initializeMarches();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.decompteId = Number(id);
        this.loadDecompte(this.decompteId);
      } else {
        this.isEditMode = false;
        this.decompteId = null;
      }
    });
  }

  initForm(): void {
    this.decompteForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      libelle: ['', Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    });
  }

  initializeMarches(): void {
    this.loadingMarches = true;
    this.marcheService.getAllMarchesForLazyLoad(0, this.rowsPerPage, 'numero,asc').subscribe({
      next: (res: PageResponse<MarcheResponseDTO>) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        this.marchesOptions = Array.from({ length: total }).map(() => ({}));

        this.marchesOptions.splice(0, data.length, ...data);
        this.loadingMarches = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec de l\'initialisation des marchés.',
          life: 3000,
        });
        this.loadingMarches = false;
      }
    });
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / this.rowsPerPage);

    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) {
      return; // Pas besoin de recharger
    }

    this.loadingMarches = true;
    this.marcheService.getAllMarches(
      { numero: this.currentFilter },
      page,
      this.rowsPerPage,
      'numero,asc'
    ).subscribe({
      next: (res: PageResponse<MarcheResponseDTO>) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        const updatedOptions = [...this.marchesOptions];

        updatedOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = updatedOptions;
        this.loadingMarches = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des marchés.',
          life: 3000,
        });
        this.loadingMarches = false;
      }
    });
  }

  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentFilter = query;

    this.loadingMarches = true;
    this.marcheService.getAllMarches(
      { numero: query },
      0,
      this.rowsPerPage,
      'numero,asc'
    ).subscribe({
      next: (res: PageResponse<MarcheResponseDTO>) => {
        const total = res.page.totalElements;
        const data = res.content.map(m => ({ label: m.numero, value: m.id }));

        this.marchesOptions = Array.from({ length: total }).map(() => ({}));
        this.marchesOptions.splice(0, data.length, ...data);
        this.loadingMarches = false;
      },
      error: () => {
        this.loadingMarches = false;
      }
    });
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/decomptes', label: 'Décomptes' },
    ];
  }

  loadDecompte(id: number): void {
    this.loading = true;
    this.decompteService.getDecompteById(id).subscribe({
      next: (decompte: DecompteResponseDto) => {
        this.marcheService.getMarcheById(decompte.marcheId).subscribe({
          next: (marche: MarcheResponseDTO) => {
            const selected = { label: marche.numero, value: marche.id };

            if (!this.marchesOptions.find(m => m?.value === marche.id)) {
              this.marchesOptions[0] = selected;
            }

            this.decompteForm.patchValue({
              marcheId: decompte.marcheId,
              dateEmission: new Date(decompte.dateEmission),
              montant: decompte.montant,
              libelle: decompte.libelle,
            });
          }
        });
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement du décompte.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/decomptes']);
      },
    });
  }

  get f() {
    return this.decompteForm.controls;
  }

  onSubmit(): void {
    this.decompteForm.markAllAsTouched();
    this.decompteForm.updateValueAndValidity();

    if (this.decompteForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce décompte ?' : 'Êtes-vous sûr de vouloir créer ce décompte ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSave();
      },
      reject: () => {
        this.messageService.add({
          severity: 'info',
          summary: 'Annulé',
          detail: 'Opération d\'enregistrement annulée.',
          life: 3000,
        });
      },
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.decompteForm.value;
    const request: DecompteRequestDto = {
      marcheId: formValue.marcheId,
      dateEmission: formValue.dateEmission,
      montant: formValue.montant,
      libelle: formValue.libelle,
    };

    const op$ = (this.isEditMode && this.decompteId)
      ? this.decompteService.updateDecompte(this.decompteId, request)
      : this.decompteService.createDecompte(request);

    op$.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: this.isEditMode ? 'Décompte mis à jour avec succès.' : 'Décompte créé avec succès.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/decomptes']);
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Échec de l\'opération.';
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: errorMessage,
          life: 3000,
        });
        this.loading = false;
      },
    });
  }

  onCancel(): void {
    this.router.navigate(['/portail/decomptes']);
  }
}
