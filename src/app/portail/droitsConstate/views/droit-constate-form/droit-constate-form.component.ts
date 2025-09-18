import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';
import { DroitConstateRequestDto } from '../../dtos/DroitConstateRequestDto';
import { DroitConstateResponseDto } from '../../dtos/DroitConstateResponseDto';
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
import {Type} from "../../../prefinancements/enums/Type";
import {DroitConstateService} from "../../service/DroitConstateService";

@Component({
  selector: 'app-droit-constate-form',
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
  templateUrl: './droit-constate-form.component.html',
  styleUrl: './droit-constate-form.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class DroitConstateFormComponent implements OnInit {
  droitConstateForm!: FormGroup;
  isEditMode: boolean = false;
  droitConstateId: number | null = null;
  loading: boolean = false;

  marchesOptions: { label: string; value: number }[] = [];
  typeOptions: { label: string; value: Type }[] = [];

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
    this.initForm();
    this.loadDropdownData();
    this.setupBreadcrumbs();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.droitConstateId = Number(id);
        this.loadDroitConstate(this.droitConstateId);
      } else {
        this.isEditMode = false;
        this.droitConstateId = null;
      }
    });

  }

  /**
   * Initializes the form with fields corresponding to the new DroitConstateRequestDto.
   */
  initForm(): void {
    this.droitConstateForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      type: [null, Validators.required],
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

  loadDropdownData(): void {
    this.loading = true;

    this.typeOptions = Object.keys(Type).map((key) => ({
      label: key,
      value: Type[key as keyof typeof Type],
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/droits-constate', label: 'Droits constatés' },
    ];
  }

  /**
   * Loads the data for an existing DroitConstate.
   */
  loadDroitConstate(id: number): void {
    this.loading = true;
    this.droitConstateService.getDroitConstateById(id).subscribe({
      next: (droitConstate: DroitConstateResponseDto) => {
        this.droitConstateForm.patchValue({
          marcheId: droitConstate.marcheId,
          dateEmission: new Date(droitConstate.dateEmission),
          montant: droitConstate.montant,
          type: droitConstate.type,
        });

        if (
          droitConstate.marcheId &&
          !this.marchesOptions.find(m => m.value === droitConstate.marcheId)
        ) {
          this.marcheService.getMarcheById(droitConstate.marcheId).subscribe({
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
          detail: 'Échec du chargement du droit constaté.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/droits-constate']);
      },
    });
  }

  get f() {
    return this.droitConstateForm.controls;
  }

  onSubmit(): void {
    this.droitConstateForm.markAllAsTouched();
    this.droitConstateForm.updateValueAndValidity();

    if (this.droitConstateForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce droit constaté ?' : 'Êtes-vous sûr de vouloir créer ce droit constaté ?',
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
    const formValue = this.droitConstateForm.value;
    const request: DroitConstateRequestDto = {
      marcheId: formValue.marcheId,
      dateEmission: formValue.dateEmission,
      montant: formValue.montant,
      type: formValue.type,
    };

    if (this.isEditMode && this.droitConstateId) {
      this.droitConstateService.updateDroitConstate(this.droitConstateId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Droit constaté mis à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/droits-constate']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la mise à jour du droit constaté.';
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
      this.droitConstateService.createDroitConstate(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Droit constaté créé avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/droits-constate']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la création du droit constaté.';
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
    this.router.navigate(['/portail/droits-constate']);
  }
}
