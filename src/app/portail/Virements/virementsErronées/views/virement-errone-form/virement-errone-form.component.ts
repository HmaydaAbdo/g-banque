import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap } from '@angular/router';


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
import { InputTextareaModule } from 'primeng/inputtextarea';
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {VirementErroneService} from "../../service/virement-errone.service";
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {PageResponse} from "../../../../global/types/PageResponse";
import {VirementErroneResponseDto} from "../../dtos/VirementErroneResponseDto";
import {VirementErroneRequestDto} from "../../dtos/VirementErroneRequestDto";

@Component({
  selector: 'app-virement-errone-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    CalendarModule,
    BreadcrumbModule,
    TooltipModule,
    InputTextareaModule,
  ],
  templateUrl: './virement-errone-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class VirementErroneFormComponent implements OnInit {
  virementForm!: FormGroup;
  isEditMode = false;
  virementId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private virementErroneService: VirementErroneService,
    private banqueService: BanqueService,
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
        this.virementId = Number(id);
        this.loadVirement(this.virementId);
      }
    });
  }

  initForm(): void {
    this.virementForm = this.fb.group({
      banqueId: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      dateEmission: [null, Validators.required],
      observation: [null, Validators.required],
      libelle: [null, Validators.required],
    });
  }

  loadBanques(event: any): void {
    this.loading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    const sort = 'nom,asc';

    this.banqueService.getAllBanques({}, page, size, sort).subscribe({
      next: (res: PageResponse<BanqueResponse>) => {
        this.banquesOptions = res.content;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-exclamation-triangle', route: '/portail/Virements/errones', label: 'Virements Erronés' },
    ];
  }

  loadVirement(id: number): void {
    this.loading = true;
    this.virementErroneService.getById(id).subscribe({
      next: (virement: VirementErroneResponseDto) => {
        this.virementForm.patchValue({
          banqueId: virement.banqueId,
          observation: virement.observation,
          libelle: virement.libelle,
          montant: virement.montant,
          dateEmission: new Date(virement.dateEmission),
        });

        if (virement.banqueId && !this.banquesOptions.find(b => b.id === virement.banqueId)) {
          this.banqueService.getBanqueById(virement.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement du virement erroné.'
        });
        this.loading = false;
        this.router.navigate(['/portail/Virements/errones']);
      },
    });
  }

  get f() {
    return this.virementForm.controls;
  }

  onSubmit(): void {
    this.virementForm.markAllAsTouched();

    if (this.virementForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs.',
        life: 3000
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode
        ? 'Êtes-vous sûr de vouloir modifier ce virement erroné ?'
        : 'Êtes-vous sûr de vouloir créer ce virement erroné ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.virementForm.value;

    const request: VirementErroneRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: formValue.dateEmission.toISOString(),
    };

    if (this.isEditMode && this.virementId) {
      this.virementErroneService.updateVirementErrone(this.virementId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Virement erroné mis à jour.',
            life: 3000
          });
          this.loading = false;
          this.router.navigate(['/portail/Virements/errones']);
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error?.message || 'Échec de la mise à jour.',
            life: 3000
          });
          this.loading = false;
        },
      });
    } else {
      this.virementErroneService.createVirementErrone(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Virement erroné créé.',
            life: 3000
          });
          this.loading = false;
          this.router.navigate(['/portail/Virements/errones']);
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: error.error?.message || 'Échec de la création.',
            life: 3000
          });
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/Virements/errones']);
  }
}
