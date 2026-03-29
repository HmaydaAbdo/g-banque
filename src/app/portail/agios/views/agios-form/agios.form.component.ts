import { Component, OnInit } from '@angular/core';
import {CommonModule, Location} from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
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
import { InputTextareaModule } from 'primeng/inputtextarea';
import { AgiosService } from "../../service/agios.service";
import { AgiosResponseDto } from "../../dtos/AgiosResponseDto";
import { AgiosRequestDto } from "../../dtos/AgiosRequestDto";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-agios-form',
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
  templateUrl: './agios.form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class AgiosFormComponent implements OnInit {
  agiosForm!: FormGroup;
  isEditMode = false;
  agiosId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];

  items: MenuItem[] | undefined;

  selectedBanque?: BanqueResponse;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location:Location,
    private agiosService: AgiosService,
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
        this.agiosId = Number(id);
        this.loadAgios(this.agiosId);
      } else {
        // Mode création : pré-sélectionner la banque si banqueId est passé en queryParam
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          this.banqueService.getBanqueById(+banqueIdParam).subscribe(banque => {
            this.banquesOptions = [banque];
            this.agiosForm.patchValue({ banqueId: banque.id });
          });
        }
      }
    });
  }

  initForm(): void {
    this.agiosForm = this.fb.group({
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
      { icon: 'pi pi-list', route: '/portail/agios', label: 'Agios' },
    ];
  }

  loadAgios(id: number): void {
    this.loading = true;
    this.agiosService.getById(id).subscribe({
      next: (agios: AgiosResponseDto) => {
        this.agiosForm.patchValue({
          banqueId: agios.banqueId,
          observation: agios.observation,
          libelle: agios.libelle,
          montant: agios.montant,
          dateEmission: parseLocalDate(agios.dateEmission),
        });

        // Ajout de la banque dans options si manquante
        if (agios.banqueId && !this.banquesOptions.find(b => b.id === agios.banqueId)) {
          this.banqueService.getBanqueById(agios.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement de l\'agios.'
        });
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.agiosForm.controls;
  }

  onSubmit(): void {
    this.agiosForm.markAllAsTouched();

    if (this.agiosForm.invalid) {
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
        ? 'Êtes-vous sûr de vouloir modifier cet agios ?'
        : 'Êtes-vous sûr de vouloir créer cet agios ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.agiosForm.value;
    const request: AgiosRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
    };

    if (this.isEditMode && this.agiosId) {
      this.agiosService.updateAgios(this.agiosId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Agios mis à jour.',
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
      this.agiosService.createAgios(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Agios créé.',
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
