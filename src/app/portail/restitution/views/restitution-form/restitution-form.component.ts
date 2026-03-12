import { Component, OnInit } from '@angular/core';
import { CommonModule,Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule, ParamMap } from '@angular/router';

import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
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
import { RestitutionService } from '../../service/restitution.service';
import { RestitutionResponseDto } from '../../dtos/RestitutionResponseDto';
import { RestitutionRequestDto } from '../../dtos/RestitutionRequestDto';
import {InputTextareaModule} from "primeng/inputtextarea";

@Component({
  selector: 'app-restitution-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
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
  templateUrl: './restitution-form.component.html',
  styleUrl: './restitution-form.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class RestitutionFormComponent implements OnInit {
  restitutionForm!: FormGroup;
  isEditMode = false;
  restitutionId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location: Location,
    private restitutionService: RestitutionService,
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
        this.restitutionId = Number(id);
        this.loadRestitution(this.restitutionId);
      }
    });
  }

  initForm(): void {
    this.restitutionForm = this.fb.group({
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
      { icon: 'pi pi-list', route: '/portail/restitutions', label: 'Restitutions' },
      {
        icon: this.isEditMode ? 'pi pi-pencil' : 'pi pi-plus',
        label: this.isEditMode ? 'Modifier' : 'Nouveau'
      }
    ];
  }

  loadRestitution(id: number): void {
    this.loading = true;
    this.restitutionService.getById(id).subscribe({
      next: (restitution: RestitutionResponseDto) => {
        this.restitutionForm.patchValue({
          banqueId: restitution.banqueId,
          observation: restitution.observation,
          libelle: restitution.libelle,
          montant: restitution.montant,
          dateEmission: new Date(restitution.dateEmission),
        });

        // Charger la banque si elle n'est pas dans la liste
        if (restitution.banqueId && !this.banquesOptions.find(b => b.id === restitution.banqueId)) {
          this.banqueService.getBanqueById(restitution.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.setupBreadcrumbs(); // Mettre à jour le breadcrumb après chargement
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement de la restitution.'
        });
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.restitutionForm.controls;
  }

  onSubmit(): void {
    this.restitutionForm.markAllAsTouched();

    if (this.restitutionForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs du formulaire.',
        life: 3000
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode
        ? 'Êtes-vous sûr de vouloir modifier cette restitution ?'
        : 'Êtes-vous sûr de vouloir créer cette restitution ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.restitutionForm.value;
    const request: RestitutionRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: formValue.dateEmission.toISOString(),
    };

    if (this.isEditMode && this.restitutionId) {
      this.restitutionService.updateRestitution(this.restitutionId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Restitution mise à jour avec succès.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
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
      this.restitutionService.createRestitution(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Restitution créée avec succès.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
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
    this.location.back();
  }
}
