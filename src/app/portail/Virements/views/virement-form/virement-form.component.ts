import { Component, OnInit } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';

// PrimeNG Modules
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import {CalendarModule} from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import {Observable, of, switchMap} from 'rxjs';
import {BanqueDto} from "../../../cmt/dtos/BanqueDto";
import {Type} from "../../Type";
import {VirementService} from "../../service/VirementService";
import {VirementResponseDto} from "../../dtos/VirementResponseDto";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {VirementRequestDto} from "../../dtos/VirementRequestDto";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {Ripple} from "primeng/ripple";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';



@Component({
  selector: 'app-virement-form',
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
    Ripple
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './virement-form.component.html',
  styleUrl: './virement-form.component.scss'
})
export class VirementFormComponent implements OnInit {

  virementForm!: FormGroup;
  isEditMode: boolean = false;
  virementId: number | null = null;
  loading: boolean = false;
  banques: BanqueDto[] = [];

  // Le type est fixé par la DTO backend à ENCAISSEMENT, mais nous allons le garder dans l'interface pour la cohérence
  virementType: Type = Type.ENCAISSEMENT;

  items: MenuItem[] = [
    { label: 'Accueil', routerLink: '/portail' },
    { label: 'Virements ( encaissements )', routerLink: '/portail/Virements/encaissement' },
  ];


  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    public router: Router,
    private banqueService: BanqueService,
    private virementService: VirementService,
    private messageService: MessageService,
    public location: Location,
  ) {
  }

  ngOnInit(): void {
    this.checkModeAndLoadData();
  }

  // Getter pour simplifier l'accès aux contrôles du formulaire dans le template HTML
  get f() {
    return this.virementForm.controls;
  }

  private initForm(virement?: VirementResponseDto): void {

    this.virementForm = this.fb.group({
      banqueBeneficiaireId: [virement ? virement.banqueBeneficiaireId : null, Validators.required],
      dateReception: [virement ? parseLocalDate(virement.dateEmission) : null, Validators.required],
      montant: [virement ? virement.montant : null, [Validators.required, Validators.min(0.01)]],
      type:[{value: this.virementType, disabled: true},Validators.required],
      beneficiaire:[{value: "STAPORT", disabled: true},Validators.required],
      emetteur: [virement ? virement.emetteur : '', Validators.required],
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
          return this.virementService.getVirementById(this.virementId);
        } else {
          this.isEditMode = false;
          // Mode ajout : initialiser le formulaire vide
          return of(null);
        }
      }),
      // 2. Charger la banque selon le contexte (édition ou création depuis les encours)
      switchMap(virementResponse => {
        if (virementResponse && virementResponse.banqueBeneficiaireId) {
          // Mode édition : charger la banque liée à l'entité
          return this.banqueService.getBanqueById(virementResponse.banqueBeneficiaireId).pipe(
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
          this.virementForm.patchValue({ banqueBeneficiaireId: this.banques[0].id });
        }
        this.loading = false;


      },
      error: (err) => {
        console.error('Erreur lors du chargement du virement:', err);
        this.loading = false;
        this.location.back();
      }
    });
  }
  loadBanques(event: any) {

    const page = event.first / event.rows || 0;
    const size = event.rows || 10;

    const criteria = { nom: event.filter }; // filtre par nom si tapé

    this.banqueService.getAllBanques(criteria, page, size).subscribe({
      next: (data) => {
        this.banques = data.content.map(b => ({ id: b.id, nom: b.nom }));
      },
      error: () => {
        console.error('Erreur lors du chargement des banques');
      }
    });
  }

  saveVirement(): void {
    if (this.virementForm.invalid) {
      this.virementForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs du formulaire.' });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête
    const raw = this.virementForm.getRawValue();
    const requestDto: VirementRequestDto = { ...raw, dateReception: toLocalDateString(raw.dateReception)! };

    let operation$: Observable<VirementResponseDto>;
    let successMessage: string;

    if (this.isEditMode && this.virementId) {
      operation$ = this.virementService.updateVirement(this.virementId, requestDto);
      successMessage = 'Virement modifié avec succès !';
    } else {
      operation$ = this.virementService.createVirement(requestDto);
      successMessage = 'Virement ajouté avec succès !';
    }

    operation$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: successMessage });
        this.location.back();
      },
      error: (err) => {
        console.error('Erreur lors de l\'enregistrement du virement:', err);
        this.loading = false;
      }
    });
  }
}
