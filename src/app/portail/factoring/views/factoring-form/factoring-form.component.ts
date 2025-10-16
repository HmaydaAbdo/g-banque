import {Component, OnInit} from "@angular/core";
import {CommonModule, NgIf} from "@angular/common";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {CalendarModule} from "primeng/calendar";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ActivatedRoute, ParamMap, Router, RouterLink, RouterLinkActive} from "@angular/router";
import {TooltipModule} from "primeng/tooltip";
import {ConfirmationService, MenuItem, MessageService} from "primeng/api";
import {Type} from "../../../prefinancements/enums/Type";
import {MarcheService} from "../../../Marches/service/marche-service.service";
import {PageResponse} from "../../../global/types/PageResponse";
import {MarcheResponseDTO} from "../../../Marches/dtos/marche-response.dto";
import {FactoringService} from "../../service/FactoringService";
import {FactoringResponseDto} from "../../dtos/FactoringResponseDto";
import {FactoringRequestDto} from "../../dtos/FactoringRequestDto";

@Component({
  selector: 'app-factoring-form',
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
  templateUrl: './factoring-form.component.html',

  providers: [MessageService, ConfirmationService],
})
export class FactoringFormComponent implements OnInit {
  factoringForm!: FormGroup;
  isEditMode: boolean = false;
  factoringId: number | null = null;
  loading: boolean = false;

  marchesOptions: { label: string; value: number }[] = [];
  typeOptions: { label: string; value: Type }[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private factoringService: FactoringService,
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
        this.factoringId = Number(id);
        this.loadFactoring(this.factoringId);
      } else {
        this.isEditMode = false;
        this.factoringId = null;
      }
    });

  }


  initForm(): void {
    this.factoringForm = this.fb.group({
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
      { icon: 'pi pi-list', route: '/portail/factoring', label: 'Factoring' },
    ];
  }


  loadFactoring(id: number): void {
    this.loading = true;
    this.factoringService.getFactoringById(id).subscribe({
      next: (factoring: FactoringResponseDto) => {
        this.factoringForm.patchValue({
          marcheId: factoring.marcheId,
          dateEmission: new Date(factoring.dateEmission),
          montant: factoring.montant,
          type: factoring.type,
        });

        if (
          factoring.marcheId &&
          !this.marchesOptions.find(m => m.value === factoring.marcheId)
        ) {
          this.marcheService.getMarcheById(factoring.marcheId).subscribe({
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
          detail: 'Échec du chargement du factoring.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/factoring']);
      },
    });
  }

  get f() {
    return this.factoringForm.controls;
  }

  onSubmit(): void {
    this.factoringForm.markAllAsTouched();
    this.factoringForm.updateValueAndValidity();

    if (this.factoringForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce factoring ?' : 'Êtes-vous sûr de vouloir créer ce factoring ?',
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
    const formValue = this.factoringForm.value;
    const request: FactoringRequestDto = {
      marcheId: formValue.marcheId,
      dateEmission: formValue.dateEmission,
      montant: formValue.montant,
      type: formValue.type,
    };

    if (this.isEditMode && this.factoringId) {
      this.factoringService.updateFactoring(this.factoringId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Factoring mis à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/factoring']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la mise à jour du factoring.';
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
      this.factoringService.createFactoring(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Factoring créé avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/factoring']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la création du factoring.';
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
    this.router.navigate(['/portail/factoring']);
  }
}
