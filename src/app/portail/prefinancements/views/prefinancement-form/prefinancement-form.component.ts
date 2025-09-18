import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';

import { PrefinancementRequestDTO } from '../../dtos/PrefinancementRequestDTO';
import { PrefinancementResponseDTO } from '../../dtos/PrefinancementResponseDTO';
import { Type } from '../../enums/Type';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { PageResponse } from '../../../global/types/PageResponse';

import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';
import {PrefinancementService} from "../../service/PrefinancementService";
import {MarcheResponseDTO} from "../../../Marches/dtos/marche-response.dto";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";

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
  loading: boolean = false;

  marchesOptions: { label: string; value: number }[] = [];
  banquesOptions: { label: string; value: number }[] = [];
  typeOptions: { label: string; value: Type }[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private prefinancementService: PrefinancementService,
    private marcheService: MarcheService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadDropdownData();
    this.setupBreadcrumbs();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.prefinancementId = Number(id);
        this.loadPrefinancement(this.prefinancementId);
      } else {
        this.isEditMode = false;
        this.prefinancementId = null;
      }
    });

  }

  /**
   * Initializes the form with fields corresponding to the new PrefinancementRequestDTO.
   */
  initForm(): void {
    this.prefinancementForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      type: [null, Validators.required],
    });
  }

  // New method to handle lazy loading of marchés
  loadMarches(event: any): void {
    this.loading = true;
    const { first, rows } = event;
    const page = first / rows;
    const size = rows;
    const sort = 'id,asc';

    this.marcheService.getAllMarchesForLazyLoad(page, size,sort ).subscribe({
      next: (pageResponse: PageResponse<MarcheResponseDTO>) => {
        this.marchesOptions = pageResponse.content.map((marche) => ({
          label: `${marche.numero}`,
          value: marche.id,
        }));
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des marchés.',
          life: 3000,
        });
        this.loading = false;
      },
    });
  }

  loadDropdownData(): void {
    this.loading = true;

    // The marches are now loaded via lazy loading
    // this.marcheService.getAllMarches({}, 0, 1000).subscribe({...})
    // No need for this call anymore.

    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (pageResponse: PageResponse<BanqueResponse>) => {
        this.banquesOptions = pageResponse.content.map((banque) => ({
          label: banque.nom,
          value: banque.id,
        }));
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des banques.',
          life: 3000,
        });
        this.loading = false;
      },
    });

    this.typeOptions = Object.keys(Type).map((key) => ({
      label: key,
      value: Type[key as keyof typeof Type],
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/préfinancements', label: 'Préfinancements' },
    ];
  }

  /**
   * Loads the data for an existing Prefinancement.
   * Note: We only patch the values that exist in the form.
   */
  loadPrefinancement(id: number): void {
    this.loading = true;
    this.prefinancementService.getPrefinancementById(id).subscribe({
      next: (prefinancement: PrefinancementResponseDTO) => {
        this.prefinancementForm.patchValue({
          marcheId: prefinancement.marcheId,
          dateEmission: new Date(prefinancement.dateEmission),
          montant: prefinancement.montant,
          type: prefinancement.type,
        });

        // 🔥 Ensure marché is added to options for edit mode
        if (
          prefinancement.marcheId &&
          !this.marchesOptions.find(m => m.value === prefinancement.marcheId)
        ) {
          this.marcheService.getMarcheById(prefinancement.marcheId).subscribe({
            next: (marche: MarcheResponseDTO) => {
              this.marchesOptions = [
                ...this.marchesOptions,
                { label: marche.numero, value: marche.id },
              ];
            },
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement du préfinancement.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/préfinancements']);
      },
    });
  }
  get f() {
    return this.prefinancementForm.controls;
  }

  onSubmit(): void {
    this.prefinancementForm.markAllAsTouched();
    this.prefinancementForm.updateValueAndValidity();

    if (this.prefinancementForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce préfinancement ?' : 'Êtes-vous sûr de vouloir créer ce préfinancement ?',
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

  /**
   * Executes the save logic based on the new, simplified DTO.
   */
  executeSave(): void {
    this.loading = true;
    const formValue = this.prefinancementForm.value;
    const request: PrefinancementRequestDTO = {
      marcheId: formValue.marcheId,
      dateEmission: formValue.dateEmission,
      montant: formValue.montant,
      type: formValue.type,
    };

    if (this.isEditMode && this.prefinancementId) {
      this.prefinancementService.updatePrefinancement(this.prefinancementId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Préfinancement mis à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/préfinancements']);
        },
        error: (error) => {
          console.log(error)
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error,
            life: 3000,
          });
          this.loading = false;
        },
      });
    } else {
      this.prefinancementService.createPrefinancement(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Préfinancement créé avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/préfinancements']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la création du préfinancement.';
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
  }

  onCancel(): void {
    this.router.navigate(['/portail/préfinancements']);
  }
}
