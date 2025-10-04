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
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
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

  marchesOptions: { label: string; value: number }[] = [];


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

  /**
   * Initializes the form with fields corresponding to the new DecompteRequestDto.
   */
  initForm(): void {
    this.decompteForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    });
  }

  // Method to handle lazy loading of marchés
  loadMarches(event: any): void {
    this.loading = true;
    const { first, rows } = event;
    const page = first / rows;
    const size = rows;
    const sort = 'id,asc';

    this.marcheService.getAllMarchesForLazyLoad(page, size, sort).subscribe({
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



  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/decomptes', label: 'Décomptes' },
    ];
  }

  /**
   * Loads the data for an existing Decompte.
   */
  loadDecompte(id: number): void {
    this.loading = true;
    this.decompteService.getDecompteById(id).subscribe({
      next: (decompte: DecompteResponseDto) => {
        this.decompteForm.patchValue({
          marcheId: decompte.marcheId,
          dateEmission: new Date(decompte.dateEmission),
          montant: decompte.montant,
        });

        if (
          decompte.marcheId &&
          !this.marchesOptions.find(m => m.value === decompte.marcheId)
        ) {
          this.marcheService.getMarcheById(decompte.marcheId).subscribe({
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

  /**
   * Executes the save logic.
   */
  executeSave(): void {
    this.loading = true;
    const formValue = this.decompteForm.value;
    const request: DecompteRequestDto = {
      marcheId: formValue.marcheId,
      dateEmission: formValue.dateEmission,
      montant: formValue.montant,
    };

    if (this.isEditMode && this.decompteId) {
      this.decompteService.updateDecompte(this.decompteId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Décompte mis à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/decomptes']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la mise à jour du décompte.';
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: errorMessage,
            life: 3000,
          });
          this.loading = false;
        },
      });
    } else {
      this.decompteService.createDecompte(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Décompte créé avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/decomptes']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la création du décompte.';
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
    this.router.navigate(['/portail/decomptes']);
  }
}
