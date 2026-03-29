import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { ImpotEtTaxeRequestDto } from '../../dtos/ImpotEtTaxeRequestDto';
import { ImpotEtTaxeResponseDto } from '../../dtos/ImpotEtTaxeResponseDto';
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
import {Motif, MotifLabels} from "../../dtos/Motif";
import {ImpotEtTaxeService} from "../../service/ImpotEtTaxeService";
import {InputTextareaModule} from "primeng/inputtextarea";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-impot-et-taxe-form',
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
  templateUrl: './impot-et-taxe-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class ImpotEtTaxeFormComponent implements OnInit {
  impotEtTaxeForm!: FormGroup;
  isEditMode = false;
  impotEtTaxeId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  motifOptions: { label: string; value: Motif | null }[] = [];

  items: MenuItem[] | undefined;
  selectedBanque?: BanqueResponse;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location:Location,
    private impotEtTaxeService: ImpotEtTaxeService,
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
        this.impotEtTaxeId = Number(id);
        this.loadImpotEtTaxe(this.impotEtTaxeId);
      } else {
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          this.banqueService.getBanqueById(+banqueIdParam).subscribe(banque => {
            this.banquesOptions = [banque];
            this.impotEtTaxeForm.patchValue({ banqueId: banque.id });
          });
        }
      }
    });
  }

  initForm(): void {
    this.impotEtTaxeForm = this.fb.group({
      banqueId: [null, Validators.required],
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
    this.motifOptions = Object.values(Motif).map(motif => ({
      label: MotifLabels[motif],
      value: motif
    }));
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/impots-taxes', label: 'Impôts et Taxes' },
    ];
  }

  loadImpotEtTaxe(id: number): void {
    this.loading = true;
    this.impotEtTaxeService.getImpotEtTaxeById(id).subscribe({
      next: (impotEtTaxe: ImpotEtTaxeResponseDto) => {
        this.impotEtTaxeForm.patchValue({
          banqueId: impotEtTaxe.banqueId,
          motif: impotEtTaxe.motif,
          observation: impotEtTaxe.observation,
          libelle: impotEtTaxe.libelle,
          montant: impotEtTaxe.montant,
          dateEmission: parseLocalDate(impotEtTaxe.dateEmission),
        });

        // Ajout de la banque dans options si manquante
        if (impotEtTaxe.banqueId && !this.banquesOptions.find(b => b.id === impotEtTaxe.banqueId)) {
          this.banqueService.getBanqueById(impotEtTaxe.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement de l\'impôt/taxe.' });
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.impotEtTaxeForm.controls;
  }

  onSubmit(): void {
    this.impotEtTaxeForm.markAllAsTouched();

    if (this.impotEtTaxeForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cet impôt/taxe ?' : 'Êtes-vous sûr de vouloir créer cet impôt/taxe ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.impotEtTaxeForm.value;
    const request: ImpotEtTaxeRequestDto = {
      banqueId: formValue.banqueId,
      motif: formValue.motif,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
    };

    if (this.isEditMode && this.impotEtTaxeId) {
      this.impotEtTaxeService.updateImpotEtTaxe(this.impotEtTaxeId, request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impôt/taxe mis(e) à jour.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la mise à jour.', life: 3000 });
          this.loading = false;
        },
      });
    } else {
      this.impotEtTaxeService.createImpotEtTaxe(request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Impôt/taxe créé(e).', life: 3000 });
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
