import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';

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
import { ProvisionService } from "../../service/provision.service";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { PageResponse } from "../../../global/types/PageResponse";
import { ProvisionResponseDto } from "../../dtos/ProvisionResponseDto";
import { ProvisionRequestDto } from "../../dtos/ProvisionRequestDto";
import { TransactionSource } from "../../../transactions/dtos/TransactionSource";
import { SensOperation } from "../../../transactions/dtos/SensOperation";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-provision-form',
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
  templateUrl: './provision-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class ProvisionFormComponent implements OnInit {
  provisionForm!: FormGroup;
  isEditMode = false;
  provisionId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  motifOptions: { label: string; value: TransactionSource }[] = [];
  sensOptions: { label: string; value: SensOperation }[] = [
    { label: 'Encaissement', value: SensOperation.ENCAISSEMENT },
    { label: 'Décaissement', value: SensOperation.DECAISSEMENT },
  ];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location: Location,
    private provisionService: ProvisionService,
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
        this.provisionId = Number(id);
        this.loadProvision(this.provisionId);
      } else {
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          this.banqueService.getBanqueById(+banqueIdParam).subscribe(banque => {
            this.banquesOptions = [banque];
            this.provisionForm.patchValue({ banqueId: banque.id });
          });
        }
      }
    });
  }

  initForm(): void {
    this.provisionForm = this.fb.group({
      banqueId: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      dateEmission: [null, Validators.required],
      motif: [null, Validators.required],
      sens: [null, Validators.required],
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
      { icon: 'pi pi-wallet', route: '/portail/provisions', label: 'Provisions' },
    ];
  }

  loadProvision(id: number): void {
    this.loading = true;
    this.provisionService.getById(id).subscribe({
      next: (provision: ProvisionResponseDto) => {
        this.provisionForm.patchValue({
          banqueId: provision.banqueId,
          observation: provision.observation,
          libelle: provision.libelle,
          montant: provision.montant,
          dateEmission: parseLocalDate(provision.dateEmission),
          motif: provision.motif,
          sens: provision.sens,
        });

        if (provision.banqueId && !this.banquesOptions.find(b => b.id === provision.banqueId)) {
          this.banqueService.getBanqueById(provision.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement de la provision:', err);
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.provisionForm.controls;
  }

  onSubmit(): void {
    this.provisionForm.markAllAsTouched();

    if (this.provisionForm.invalid) {
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
        ? 'Êtes-vous sûr de vouloir modifier cette provision ?'
        : 'Êtes-vous sûr de vouloir créer cette provision ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.provisionForm.value;

    const request: ProvisionRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
      motif: formValue.motif,
      sens: formValue.sens,
    };

    if (this.isEditMode && this.provisionId) {
      this.provisionService.updateProvision(this.provisionId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Provision mise à jour.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          console.error('Erreur lors de la mise à jour de la provision:', error);
          this.loading = false;
        },
      });
    } else {
      this.provisionService.createProvision(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Provision créée.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          console.error('Erreur lors de la création de la provision:', error);
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.location.back();
  }
}
