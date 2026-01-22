import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap } from '@angular/router';

import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import { Status } from '../../../prefinancements/enums/Status';


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
import {PrelevementService} from "../../service/prelevement.service";
import {PrelevementResponseDto} from "../../dtos/PrelevementResponseDto";
import {PrelevementRequestDto} from "../../dtos/PrelevementRequestDto";
import {InputTextareaModule} from "primeng/inputtextarea";


@Component({
  selector: 'app-prelevement-form',
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
  templateUrl: './prelevement-form.component.html',
  styleUrl: './prelevement-form.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class PrelevementFormComponent implements OnInit {
  prelevementForm!: FormGroup;
  isEditMode = false;
  prelevementId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] | undefined;

  selectedBanque?: BanqueResponse;


  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private prelevementService: PrelevementService,
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
        this.prelevementId = Number(id);
        this.loadPrelevement(this.prelevementId);
      }
    });
  }

  initForm(): void {
    this.prelevementForm = this.fb.group({
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
      { icon: 'pi pi-list', route: '/portail/prelevements', label: 'Prélèvements' }, // Changement de label et route
    ];
  }

  loadPrelevement(id: number): void {
    this.loading = true;
    this.prelevementService.getById(id).subscribe({
      next: (prelevement: PrelevementResponseDto) => {
        this.prelevementForm.patchValue({
          banqueId: prelevement.banqueId,
          observation: prelevement.observation,
          libelle: prelevement.libelle,
          montant: prelevement.montant,
          dateEmission: new Date(prelevement.dateEmission),
        });


        if (prelevement.banqueId && !this.banquesOptions.find(b => b.id === prelevement.banqueId)) {
          this.banqueService.getBanqueById(prelevement.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement du prélèvement.' });
        this.loading = false;
        this.router.navigate(['/portail/prelevements']);
      },
    });
  }

  get f() {
    return this.prelevementForm.controls;
  }

  onSubmit(): void {
    this.prelevementForm.markAllAsTouched();

    if (this.prelevementForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de cette modification ?' : 'Êtes-vous sûr de vouloir créer le prélèvement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.prelevementForm.value;
    const request: PrelevementRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: formValue.dateEmission,
    };

    if (this.isEditMode && this.prelevementId) {
      this.prelevementService.updatePrelevement(this.prelevementId, request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Prélèvement mis à jour.', life: 3000 });
          this.loading = false;
          this.router.navigate(['/portail/prelevements']);
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la mise à jour.', life: 3000 });
          this.loading = false;
        },
      });
    } else {
      this.prelevementService.createPrelevement(request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Prélèvement créé.', life: 3000 });
          this.loading = false;
          this.router.navigate(['/portail/prelevements']);
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la création.', life: 3000 });
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/prelevements']);
  }
}
