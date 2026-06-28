import { Component, inject, OnInit } from '@angular/core';
import { Location, NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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

import { AgiosService } from '../../service/agios.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { AgiosResponseDto } from '../../dtos/AgiosResponseDto';
import { AgiosRequestDto } from '../../dtos/AgiosRequestDto';
import { PageResponse } from '../../../global/types/PageResponse';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-agios-form',
  standalone: true,
  imports: [
    NgClass,
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

  private readonly fb                  = inject(FormBuilder);
  private readonly route               = inject(ActivatedRoute);
  private readonly location            = inject(Location);
  private readonly agiosService        = inject(AgiosService);
  private readonly banqueService       = inject(BanqueService);
  private readonly messageService      = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  agiosForm = this.fb.group({
    banqueId:     [null as number | null, Validators.required],
    montant:      [null as number | null, [Validators.required, Validators.min(0.01)]],
    dateEmission: [null as Date | null,   Validators.required],
    observation:  [null as string | null, Validators.required],
    libelle:      [null as string | null, Validators.required],
  });

  isEditMode = false;
  agiosId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  items: MenuItem[] | undefined;

  ngOnInit(): void {
    this.setupBreadcrumbs();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.agiosId    = Number(id);
        this.loadAgios(this.agiosId);
      } else {
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

  loadBanques(event: any): void {
    this.loading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    this.banqueService.getAllBanques({}, page, size, 'nom,asc').subscribe({
      next: (res: PageResponse<BanqueResponse>) => {
        this.banquesOptions = res.content;
        this.loading = false;
      },
      error: () => { this.loading = false; }
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
          banqueId:     agios.banqueId,
          observation:  agios.observation,
          libelle:      agios.libelle,
          montant:      agios.montant,
          dateEmission: parseLocalDate(agios.dateEmission),
        });
        if (agios.banqueId && !this.banquesOptions.find(b => b.id === agios.banqueId)) {
          this.banqueService.getBanqueById(agios.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.location.back();
      }
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
      banqueId:     formValue.banqueId!,
      libelle:      formValue.libelle ?? undefined,
      observation:  formValue.observation ?? undefined,
      montant:      formValue.montant!,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
    };

    if (this.isEditMode && this.agiosId) {
      this.agiosService.updateAgios(this.agiosId, request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Agios mis à jour.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: () => { this.loading = false; }
      });
    } else {
      this.agiosService.createAgios(request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Agios créé.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: () => { this.loading = false; }
      });
    }
  }

  onCancel(): void {
    this.location.back();
  }
}
