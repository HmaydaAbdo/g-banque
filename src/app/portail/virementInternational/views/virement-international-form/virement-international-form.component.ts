import { Component, OnInit } from '@angular/core';
import { CommonModule,Location } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { Observable, of, switchMap } from 'rxjs';

// PrimeNG Modules
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';

import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { Type } from '../../../Virements/Type';
import { VirementInternationalService } from '../../service/virement-international.service';
import { VirementInternationalResponseDto } from '../../dtos/VirementInternationalResponseDto';
import { VirementInternationalRequestDto } from '../../dtos/VirementInternationalRequestDto';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-virement-international-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    DropdownModule,
    InputTextModule,
    CalendarModule,
    InputNumberModule,
    ButtonModule,
    ToastModule,
    BreadcrumbModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
    RippleModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './virement-international-form.component.html'
})
export class VirementInternationalFormComponent implements OnInit {

  virementForm!: FormGroup;
  isEditMode = false;
  virementId: number | null = null;
  loading = false;
  banques: BanqueResponse[] = [];

  typeOptions = [
    { label: 'Encaissement', value: Type.ENCAISSEMENT },
    { label: 'Décaissement', value: Type.DECAISSEMENT }
  ];

  items: MenuItem[] = [
    { label: 'Accueil', routerLink: '/portail' },
    { label: 'Virements Internationaux', routerLink: '/portail/virements-internationaux' },
    { label: 'Formulaire' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private banqueService: BanqueService,
    private virementService: VirementInternationalService,
    private messageService: MessageService,
    public router: Router,
    public location:Location
  ) {
  }

  ngOnInit(): void {
    this.checkModeAndLoadData();
  }

  // Getter pour simplifier l'accès aux contrôles du formulaire dans le template HTML
  get f() {
    return this.virementForm.controls;
  }

  private initForm(virement?: VirementInternationalResponseDto): void {
    this.virementForm = this.fb.group({
      banqueId: [virement ? virement.banqueId : null, Validators.required],
      dateEmission: [virement ? parseLocalDate(virement.dateEmission) : null, Validators.required],
      montant: [virement ? virement.montant : null, [Validators.required, Validators.min(0.01)]],
      type: [virement ? virement.type : null, Validators.required],
      beneficiaire: [virement ? virement.beneficiaire : '', Validators.required],
      emetteur: [virement ? virement.emetteur : '', Validators.required]
    });
  }

  private checkModeAndLoadData(): void {
    this.route.paramMap.pipe(
      // 1. Vérifier si un ID est présent et charger les données du virement
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          this.virementId = +id;
          this.isEditMode = true;
          this.loading = true;
          // Charger les données du virement
          return this.virementService.getVirementInternationalById(this.virementId);
        } else {
          this.isEditMode = false;
          // Mode ajout : initialiser le formulaire vide
          return of(null);
        }
      }),
      // 2. Charger la banque selon le contexte (édition ou création depuis les encours)
      switchMap(virementResponse => {
        if (virementResponse && virementResponse.banqueId) {
          // Mode édition : charger la banque liée à l'entité
          return this.banqueService.getBanqueById(virementResponse.banqueId).pipe(
            switchMap(banque => {
              this.banques = [banque];
              return of(virementResponse);
            })
          );
        }
        // Mode création : pré-sélectionner la banque si banqueId est passé en queryParam
        const banqueIdParam = this.route.snapshot.queryParamMap.get('banqueId');
        if (banqueIdParam) {
          return this.banqueService.getBanqueById(+banqueIdParam).pipe(
            switchMap(banque => {
              this.banques = [banque];
              return of(null);
            })
          );
        }
        return of(virementResponse);
      })
    ).subscribe({
      next: (virementResponse) => {
        // 3. Initialiser le formulaire avec le virement et la banque pré-chargée
        this.initForm(virementResponse || undefined);
        // 4. En mode création, pré-sélectionner la banque chargée depuis le queryParam
        if (!this.isEditMode && this.banques.length > 0) {
          this.virementForm.patchValue({ banqueId: this.banques[0].id });
        }
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement du virement international.'
        });
        this.loading = false;
        this.location.back();
      }
    });
  }

  loadBanques(event: any): void {
    const page = event.first / event.rows || 0;
    const size = event.rows || 10;

    const criteria = { nom: event.filter }; // filtre par nom si tapé

    this.banqueService.getAllBanques(criteria, page, size).subscribe({
      next: (data) => {
        this.banques = data.content;
      },
      error: () => {
        console.error('Erreur lors du chargement des banques');
      }
    });
  }

  saveVirement(): void {
    if (this.virementForm.invalid) {
      this.virementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez corriger les erreurs du formulaire.'
      });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête
    const raw = this.virementForm.getRawValue();
    const requestDto: VirementInternationalRequestDto = { ...raw, dateEmission: toLocalDateString(raw.dateEmission)! };

    let operation$: Observable<VirementInternationalResponseDto>;
    let successMessage: string;

    if (this.isEditMode && this.virementId) {
      operation$ = this.virementService.updateVirementInternational(this.virementId, requestDto);
      successMessage = 'Virement international modifié avec succès !';
    } else {
      operation$ = this.virementService.createVirementInternational(requestDto);
      successMessage = 'Virement international ajouté avec succès !';
    }

    operation$.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: successMessage
        });
          this.location.back();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.message || 'Échec de l\'opération.'
        });
        this.loading = false;
      }
    });
  }
}
