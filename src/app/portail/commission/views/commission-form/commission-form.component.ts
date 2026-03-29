import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { CommissionRequestDto } from '../../dtos/CommissionRequestDto';
import { CommissionResponseDto } from '../../dtos/commission-response.dto';
import { CommissionService } from '../../service/commission.service';
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
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { Status } from '../../../prefinancements/enums/Status';
import {InputTextareaModule} from "primeng/inputtextarea";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-commission-form',
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
  templateUrl: './commission-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class CommissionFormComponent implements OnInit {
  commissionForm!: FormGroup;
  isEditMode = false;
  commissionId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  operationOptions: { label: string; value: TransactionSource | null }[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] | undefined;

  selectedBanque?: BanqueResponse;


  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location: Location,
    private commissionService: CommissionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupBreadcrumbs();
    this.loadOperationOptions();


    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.commissionId = Number(id);
        this.loadCommission(this.commissionId);
      } else {
        // Mode création : pré-sélectionner la banque si banqueId est passé en queryParam
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          this.banqueService.getBanqueById(+banqueIdParam).subscribe(banque => {
            this.banquesOptions = [banque];
            this.commissionForm.patchValue({ banqueId: banque.id });
          });
        }
      }
    });
  }

  initForm(): void {
    this.commissionForm = this.fb.group({
      banqueId: [null, Validators.required],
      operation: [null, Validators.required],
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

  loadOperationOptions(): void {
    this.operationOptions = Object.values(TransactionSource).map(op => ({ label: op, value: op }));
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
          banqueId: commission.banqueId,
          operation: commission.operation,
          observation: commission.observation,
          libelle: commission.libelle,
          montant: commission.montant,
          dateEmission: parseLocalDate(commission.dateEmission),
        });

        // Ajout de la banque dans options si manquante
        if (commission.banqueId && !this.banquesOptions.find(b => b.id === commission.banqueId)) {
          this.banqueService.getBanqueById(commission.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement de la commission.' });
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.commissionForm.controls;
  }

  onSubmit(): void {
    this.commissionForm.markAllAsTouched();

    if (this.commissionForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette commission ?' : 'Êtes-vous sûr de vouloir créer cette commission ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.commissionForm.value;
    const request: CommissionRequestDto = {
      banqueId: formValue.banqueId,
      operation: formValue.operation,
      libelle: formValue.libelle,
      observation:formValue.observation,
      montant: formValue.montant,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
    };

    if (this.isEditMode && this.commissionId) {
      this.commissionService.updateCommission(this.commissionId, request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Commission mise à jour.', life: 3000 });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la mise à jour.', life: 3000 });
          this.loading = false;
        },
      });
    } else {
      this.commissionService.createCommission(request).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Commission créée.', life: 3000 });
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
