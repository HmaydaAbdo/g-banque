import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';


import { CommissionResponseDto } from '../../dtos/commission-response.dto';
import { TransactionSource } from '../../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { PageResponse } from '../../../global/types/PageResponse';
import { CommissionService } from '../../service/commission.service';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';

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
import {CommissionRequestDto} from "../../dtos/CommissionRequestDto";

@Component({
  selector: 'app-commission-form',
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
  templateUrl: './commission-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class CommissionFormComponent implements OnInit {
  commissionForm!: FormGroup;
  isEditMode: boolean = false;
  commissionId: number | null = null;
  loading: boolean = false;

  banquesOptions: { label: string; value: number }[] = [];
  operationOptions: { label: string; value: TransactionSource }[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private commissionService: CommissionService,
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
        this.commissionId = Number(id);
        this.loadCommission(this.commissionId);
      } else {
        this.isEditMode = false;
        this.commissionId = null;
      }
    });
  }

  initForm(): void {
    this.commissionForm = this.fb.group({
      libelle: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
      banqueId: [null, Validators.required],
      operation: [null, Validators.required],
      dateEmission: [null, Validators.required],
    });
  }

  loadDropdownData(): void {
    this.loading = true;
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

    this.operationOptions = Object.values(TransactionSource).map((key) => ({
      label: key,
      value: TransactionSource[key as keyof typeof TransactionSource],
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/commissions', label: 'Commissions' },
    ];
  }

  loadCommission(id: number): void {
    this.loading = true;
    this.commissionService.getCommissionById(id).subscribe({
      next: (commission: CommissionResponseDto) => {
        this.commissionForm.patchValue({
          libelle: commission.libelle,
          montant: commission.montant,
          banqueId: commission.banqueId,
          operation: commission.operation,
          dateEmission: new Date(commission.dateEmission),
        });

        if (
          commission.banqueId &&
          !this.banquesOptions.find(b => b.value === commission.banqueId)
        ) {
          this.banqueService.getBanqueById(commission.banqueId).subscribe({
            next: (banque: BanqueResponse) => {
              this.banquesOptions = [
                ...this.banquesOptions,
                { label: banque.nom, value: banque.id },
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
          detail: 'Échec du chargement de la commission.',
          life: 3000,
        });
        this.loading = false;
        this.router.navigate(['/portail/commissions']);
      },
    });
  }

  get f() {
    return this.commissionForm.controls;
  }

  onSubmit(): void {
    this.commissionForm.markAllAsTouched();
    this.commissionForm.updateValueAndValidity();

    if (this.commissionForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette commission ?' : 'Êtes-vous sûr de vouloir créer cette commission ?',
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
    const formValue = this.commissionForm.value;
    const request: CommissionRequestDto = {
      libelle: formValue.libelle,
      montant: formValue.montant,
      banqueId: formValue.banqueId,
      operation: formValue.operation,
      dateEmission: formValue.dateEmission,
    };

    if (this.isEditMode && this.commissionId) {
      this.commissionService.updateCommission(this.commissionId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Commission mise à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/commissions']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la mise à jour de la commission.';
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
      this.commissionService.createCommission(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Commission créée avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/commissions']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la création de la commission.';
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
    this.router.navigate(['/portail/commissions']);
  }
}
