import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import { FraisBancaireRequestDto } from '../../dtos/FraisBancaireRequestDto';
import { FraisBancaireResponseDto } from '../../dtos/FraisBancaireResponseDto';
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
import { FraisBancaireService } from "../../service/FraisBancaireService";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-frais-bancaire-form',
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
  templateUrl: './frais-bancaire-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class FraisBancaireFormComponent implements OnInit {
  fraisForm!: FormGroup;
  isEditMode = false;
  fraisId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private location:Location,
    private fraisService: FraisBancaireService,
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
        this.fraisId = Number(id);
        this.loadFraisBancaire(this.fraisId);
      } else {
        // Mode création : pré-sélectionner la banque si banqueId est passé en queryParam
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          this.banqueService.getBanqueById(+banqueIdParam).subscribe(banque => {
            this.banquesOptions = [banque];
            this.fraisForm.patchValue({ banqueId: banque.id });
          });
        }
      }
    });
  }

  initForm(): void {
    this.fraisForm = this.fb.group({
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
      { icon: 'pi pi-list', route: '/portail/frais-bancaire', label: 'Frais Bancaires' },
    ];
  }

  loadFraisBancaire(id: number): void {
    this.loading = true;
    this.fraisService.getById(id).subscribe({
      next: (frais: FraisBancaireResponseDto) => {
        this.fraisForm.patchValue({
          banqueId: frais.banqueId,
          observation: frais.observation,
          libelle: frais.libelle,
          montant: frais.montant,
          dateEmission: parseLocalDate(frais.dateEmission),
        });

        // Ajouter la banque si manquante
        if (frais.banqueId && !this.banquesOptions.find(b => b.id === frais.banqueId)) {
          this.banqueService.getBanqueById(frais.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du frais bancaire:', err);
        this.loading = false;
        this.location.back();
      },
    });
  }

  get f() {
    return this.fraisForm.controls;
  }

  onSubmit(): void {
    this.fraisForm.markAllAsTouched();

    if (this.fraisForm.invalid) {
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
        ? 'Êtes-vous sûr de vouloir modifier ce frais bancaire ?'
        : 'Êtes-vous sûr de vouloir créer ce frais bancaire ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.fraisForm.value;
    const request: FraisBancaireRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      dateEmission: toLocalDateString(formValue.dateEmission)!,
    };

    if (this.isEditMode && this.fraisId) {
      this.fraisService.updateFraisBancaire(this.fraisId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Frais bancaire mis à jour.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          console.error('Erreur lors de la mise à jour du frais bancaire:', error);
          this.loading = false;
        },
      });
    } else {
      this.fraisService.createFraisBancaire(request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Frais bancaire créé.',
            life: 3000
          });
          this.loading = false;
          this.location.back();
        },
        error: (error) => {
          console.error('Erreur lors de la création du frais bancaire:', error);
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.location.back();
  }
}
