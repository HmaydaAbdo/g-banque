import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {BanqueDto} from "../../../cmt/dtos/BanqueDto";
import {Type} from "../../../Virements/Type";
import {MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {ActivatedRoute, Router, RouterLinkActive} from "@angular/router";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {Observable, of, switchMap} from "rxjs";
import {MiseADispositionService} from "../../service/MiseADispositionService";
import {MiseADispositionResponseDto} from "../../dtos/MiseADispositionResponseDto";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ButtonDirective} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {InputTextModule} from "primeng/inputtext";
import {NgClass, NgIf} from "@angular/common";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {Ripple} from "primeng/ripple";
import {ToastModule} from "primeng/toast";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-mise-a-disposition-form',
  standalone: true,
  imports: [
    BreadcrumbModule,
    ButtonDirective,
    CalendarModule,
    ConfirmDialogModule,
    DropdownModule,
    FormsModule,
    InputNumberModule,
    InputTextModule,
    NgIf,
    PrimeTemplate,
    ProgressSpinnerModule,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    ToastModule,
    NgClass
  ],
  templateUrl: './mise-a-disposition-form.component.html',
  styleUrl: './mise-a-disposition-form.component.scss'
})
export class MiseADispositionFormComponent implements OnInit {

  miseAdispoForm!: FormGroup;
  isEditMode: boolean = false;
  miseAdispoId: number | null = null;
  loading: boolean = false;
  banques: BanqueDto[] = [];


  miseAdispoType: Type = Type.ENCAISSEMENT;
  beneficiaireFixe: string = "STAPORT";

  items: MenuItem[] = [
    { label: 'Accueil', routerLink: '/portail' },
    { label: 'Mise à dispositions  ( Encaissements )', routerLink: '/portail/Mise-a-disposition/encaissement' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private banqueService: BanqueService,
    private miseAdispoService: MiseADispositionService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.checkModeAndLoadData();
  }

  // Getter pour simplifier l'accès aux contrôles du formulaire
  get f() {
    return this.miseAdispoForm.controls;
  }

  private initForm(miseAdispo?: MiseADispositionResponseDto): void {

    this.miseAdispoForm = this.fb.group({
      banqueId: [miseAdispo ? miseAdispo.banqueId : null, Validators.required],
      dateEmission: [miseAdispo ? parseLocalDate(miseAdispo.dateEmission) : null, Validators.required],
      montant: [miseAdispo ? miseAdispo.montant : null, [Validators.required, Validators.min(0.01)]],
      emetteur: [miseAdispo ? miseAdispo.emetteur : '', Validators.required],
      beneficiaire: [{ value: this.beneficiaireFixe, disabled: true }, Validators.required],
      type: [{ value: this.miseAdispoType, disabled: true }, Validators.required],
    });
  }


  private checkModeAndLoadData(): void {
    this.route.paramMap.pipe(
      // 1. Vérifier si un ID est présent dans l'URL et charger la mise à dispo si nécessaire
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          this.miseAdispoId = +id;
          this.isEditMode = true;
          this.loading = true;
          return this.miseAdispoService.getMiseAdispositionById(this.miseAdispoId);
        } else {
          this.isEditMode = false;
          return of(null);
        }
      }),
      // 2. Charger la banque selon le contexte (édition ou création depuis les encours)
      switchMap(miseAdispoResponse => {
        if (miseAdispoResponse && miseAdispoResponse.banqueId) {
          // Mode édition : charger la banque liée à l'entité
          return this.banqueService.getBanqueById(miseAdispoResponse.banqueId).pipe(
            switchMap(banque => {
              this.banques = [banque];
              return of(miseAdispoResponse);
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
        return of(miseAdispoResponse);
      })
    ).subscribe({
      next: (miseAdispoResponse) => {
        // 3. Initialiser le formulaire une fois toutes les données chargées
        this.initForm(miseAdispoResponse || undefined);
        // 4. En mode création, pré-sélectionner la banque chargée depuis le queryParam
        if (!this.isEditMode && this.banques.length > 0) {
          this.miseAdispoForm.patchValue({ banqueId: this.banques[0].id });
        }
        this.loading = false;

        // Mettre à jour le breadcrumb
        this.items.push({
          label: this.isEditMode ? 'Modifier' : 'Ajouter',
        });
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement de la mise à disposition ' });
        this.loading = false;
        // Redirection en cas d'erreur de chargement
        this.router.navigate(['/portail/Mise-a-disposition/encaissement']);
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

  saveMiseADispo(): void {
    if (this.miseAdispoForm.invalid) {
      this.miseAdispoForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs du formulaire.' });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête en utilisant getRawValue() pour inclure les champs désactivés (beneficiaire, type)
    const raw = this.miseAdispoForm.getRawValue();
    const requestDto = { ...raw, dateEmission: toLocalDateString(raw.dateEmission)! };


    let operation$: Observable<MiseADispositionResponseDto>;
    let successMessage: string;

    if (this.isEditMode && this.miseAdispoId) {
      operation$ = this.miseAdispoService.updateMiseAdisposition(this.miseAdispoId, requestDto);
      successMessage = 'Mise à disposition  modifiée avec succès !';
    } else {
      operation$ = this.miseAdispoService.createMiseAdisposition(requestDto);
      successMessage = 'mise à disposition ajoutée avec succès !';
    }

    operation$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: successMessage });
        // Rediriger après un petit délai
        this.router.navigate(['/portail/Mise-a-disposition/encaissement'])
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message || 'Échec de l\'opération.' });
        this.loading = false;
      }
    });
  }
}
