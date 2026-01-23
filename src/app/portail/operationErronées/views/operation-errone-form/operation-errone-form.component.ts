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

import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { OperationErroneService } from "../../service/operation-errone.service";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { PageResponse } from "../../../global/types/PageResponse";
import { OperationErroneResponseDto } from "../../dtos/OperationErroneResponseDto";
import { OperationErroneRequestDto } from "../../dtos/OperationErroneRequestDto";
import { TransactionSource } from "../../../transactions/dtos/TransactionSource";

@Component({
  selector: 'app-operation-errone-form',
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
  templateUrl: './operation-errone-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class OperationErroneFormComponent implements OnInit {
  operationForm!: FormGroup;
  isEditMode = false;
  operationId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  motifOptions: { label: string; value: TransactionSource }[] = [];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private operationErroneService: OperationErroneService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadMotifOptions();
    this.setupBreadcrumbs();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.operationId = Number(id);
        this.loadOperation(this.operationId);
      }
    });
  }

  initForm(): void {
    this.operationForm = this.fb.group({
      banqueId: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      dateEmission: [null, Validators.required],
      motif: [null, Validators.required],
      observation: [null, Validators.required],
      libelle: [null, Validators.required],
    });
  }

  loadMotifOptions(): void {
    this.motifOptions = Object.values(TransactionSource).map(m => ({
      label: this.formatMotifLabel(m),
      value: m
    }));
  }

  formatMotifLabel(motif: TransactionSource): string {
    return motif.replace(/_/g, ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
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
      { icon: 'pi pi-exclamation-triangle', route: '/portail/operations/errones', label: 'Opérations Erronées' },
    ];
  }

  loadOperation(id: number): void {
    this.loading = true;
    this.operationErroneService.getById(id).subscribe({
      next: (operation: OperationErroneResponseDto) => {
        this.operationForm.patchValue({
          banqueId: operation.banqueId,
          observation: operation.observation,
          libelle: operation.libelle,
          montant: operation.montant,
          dateEmission: new Date(operation.dateEmission),
          motif: operation.motif,
        });

        if (operation.banqueId && !this.banquesOptions.find(b => b.id === operation.banqueId)) {
          this.banqueService.getBanqueById(operation.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement de l\'opération erronée.'
        });
        this.loading = false;
        this.router.navigate(['/portail/operations/errones']);
      },
    });
  }

  get f() {
    return this.operationForm.controls;
  }

  onSubmit(): void {
    this.operationForm.markAllAsTouched();

    if (this.operationForm.invalid) {
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
        ? 'Êtes-vous sûr de vouloir modifier cette opération erronée ?'
        : 'Êtes-vous sûr de vouloir créer cette opération erronée ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.operationForm.value;

    const request: OperationErroneRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: formValue.dateEmission.toISOString(),
      motif: formValue.motif,
    };

    if (this.isEditMode && this.operationId) {
      this.operationErroneService.updateOperationErrone(this.operationId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Opération erronée mise à jour.',
            life: 3000
          });
          this.loading = false;
          this.router.navigate(['/portail/operations/errones']);
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
      this.operationErroneService.createOperationErrone(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Opération erronée créée.',
            life: 3000
          });
          this.loading = false;
          this.router.navigate(['/portail/operations/errones']);
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
    this.router.navigate(['/portail/operations/errones']);
  }
}
