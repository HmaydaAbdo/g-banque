import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { ImpayeeRequestDto } from '../../dtos/ImpayeeRequestDto';
import { ImpayeeResponseDto } from '../../dtos/ImpayeeResponseDto';
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
import {TransactionSource, TransactionSourceLabels} from "../../../transactions/dtos/TransactionSource";
import { ImpayeeService } from '../../service/ImpayeeService';
import { InputTextareaModule } from 'primeng/inputtextarea';

@Component({
  selector: 'app-impayee-form',
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
  templateUrl: './impayee-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class ImpayeeFormComponent implements OnInit {
  impayeeForm!: FormGroup;
  isEditMode = false;
  impayeeId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  motifOptions: { label: string; value: TransactionSource | null }[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location:Location,
    private impayeeService: ImpayeeService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupBreadcrumbs();
    this.loadMotifOptions();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.impayeeId = Number(id);
        this.loadImpayee(this.impayeeId);
      }
    });
  }

  initForm(): void {
    this.impayeeForm = this.fb.group({
      banqueEncaissementId: [null, Validators.required],
      motif: [null, Validators.required],
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

  loadMotifOptions(): void {
    this.motifOptions = Object.values(TransactionSource).map(motif => ({
      label: TransactionSourceLabels[motif],
      value: motif
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/impayees', label: 'Impayées' },
    ];
  }

  loadImpayee(id: number): void {
    this.loading = true;
    this.impayeeService.getImpayeeById(id).subscribe({
      next: (impayee: ImpayeeResponseDto) => {
        this.impayeeForm.patchValue({
          banqueEncaissementId: impayee.banqueEncaissementId,
          motif: impayee.motif,
          observation: impayee.observation,
          libelle: impayee.libelle,
          montant: impayee.montant,
          dateEmission: new Date(impayee.dateEmission),
        });

        // Ajout de la banque dans options si manquante
        if (impayee.banqueEncaissementId && !this.banquesOptions.find(b => b.id === impayee.banqueEncaissementId)) {
          this.banqueService.getBanqueById(impayee.banqueEncaissementId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement de l\'impayée.' });
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.impayeeForm.controls;
  }

  onSubmit(): void {
    this.impayeeForm.markAllAsTouched();

    if (this.impayeeForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette impayée ?' : 'Êtes-vous sûr de vouloir créer cette impayée ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.impayeeForm.value;
    const request: ImpayeeRequestDto = {
      banqueEncaissementId: formValue.banqueEncaissementId,
      motif: formValue.motif,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: formValue.dateEmission,
    };

    if (this.isEditMode && this.impayeeId) {
      this.impayeeService.updateImpayee(this.impayeeId, request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impayée mise à jour.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la mise à jour.', life: 3000 });
          this.loading = false;
        },
      });
    } else {
      this.impayeeService.createImpayee(request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impayée créée.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la création.', life: 3000 });
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.location.back();
  }
}
