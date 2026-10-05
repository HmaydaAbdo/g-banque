import {Component, OnInit} from '@angular/core';
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ButtonDirective} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {InputTextModule} from "primeng/inputtext";
import {NgClass, Location} from "@angular/common";
import {PaginatorModule} from "primeng/paginator";
import {MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Ripple} from "primeng/ripple";
import {ActivatedRoute, Router, RouterLinkActive} from "@angular/router";
import {ToastModule} from "primeng/toast";
import {BanqueDto} from "../../../cmt/dtos/BanqueDto";
import {Type} from "../../../Virements/Type";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {Observable, of, switchMap} from "rxjs";
import {LcnService} from "../../service/LcnService";
import {LcnResponseDTO} from "../../dtos/LcnResponseDTO";
import {NatureLcn} from "../../dtos/NatureLcn";
import {RadioButtonModule} from "primeng/radiobutton";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-lcn-form',
  standalone: true,
  imports: [
    BreadcrumbModule,
    ButtonDirective,
    CalendarModule,
    ConfirmDialogModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    PaginatorModule,
    PrimeTemplate,
    ProgressSpinnerModule,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    ToastModule,
    NgClass,
    RadioButtonModule
  ],
  templateUrl: './lcn-form.component.html',
  styleUrl: './lcn-form.component.scss'
})
export class LcnFormComponent implements OnInit {

  lcnForm!: FormGroup;
  isEditMode: boolean = false;
  lcnId: number | null = null;
  loading: boolean = false;
  banques: BanqueDto[] = [];

  lcnType: Type = Type.ENCAISSEMENT;
  beneficiaireFixe: string = "STAPORT";

  natureLcnOptions: { label: string, value: NatureLcn }[] = [
    { label: 'Encaissement', value: NatureLcn.ENCAISSEMENT },
    { label: 'Escompte', value: NatureLcn.ESCOMPTE }
  ];
  items: MenuItem[] = [
    { label: 'Accueil', routerLink: '/portail' },
    { label: 'Lcn ( Encaissements )', routerLink: '/portail/LCN/encaissements' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private banqueService: BanqueService,
    private lcnService: LcnService,
    private messageService: MessageService,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.checkModeAndLoadData();
  }

  // Getter pour simplifier l'accès aux contrôles du formulaire
  get f() {
    return this.lcnForm.controls;
  }

  private initForm(lcn?: LcnResponseDTO): void {

    this.lcnForm = this.fb.group({
      numero: [lcn ? lcn.numero : null, Validators.required],
      banqueId: [lcn ? lcn.banqueId : null, Validators.required],
      dateEmission: [lcn ? parseLocalDate(lcn.dateEmission) : null, Validators.required],
      dateEcheance: [lcn ? parseLocalDate(lcn.dateEcheance) : null,Validators.required],
      montant: [lcn ? lcn.montant : null, [Validators.required, Validators.min(0.01)]],
      emetteur: [lcn ? lcn.emetteur : '', Validators.required],
      beneficiaire: [{ value: this.beneficiaireFixe, disabled: true }, Validators.required],
      type: [{ value: this.lcnType, disabled: true }, Validators.required],
      natureLcn: [lcn ? lcn.natureLcn : null, Validators.required],
    });
  }


  private checkModeAndLoadData(): void {
    this.route.paramMap.pipe(
      // 1. Vérifier si un ID est présent dans l'URL et charger le lcn si nécessaire
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          this.lcnId = +id;
          this.isEditMode = true;
          this.loading = true;
          return this.lcnService.getLcnById(this.lcnId);
        } else {
          this.isEditMode = false;
          return of(null);
        }
      }),
      // 2. Charger la banque selon le contexte (édition ou création depuis les encours)
      switchMap(lcnResponse => {
        if (lcnResponse && lcnResponse.banqueId) {
          // Mode édition : charger la banque liée à l'entité
          return this.banqueService.getBanqueById(lcnResponse.banqueId).pipe(
            switchMap(banque => {
              this.banques = [banque];
              return of(lcnResponse);
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
        return of(lcnResponse);
      })
    ).subscribe({
      next: (lcnResponse) => {
        // 3. Initialiser le formulaire une fois toutes les données chargées
        this.initForm(lcnResponse || undefined);
        // 4. En mode création, pré-sélectionner la banque chargée depuis le queryParam
        if (!this.isEditMode && this.banques.length > 0) {
          this.lcnForm.patchValue({ banqueId: this.banques[0].id });
        }
        this.loading = false;

      },
      error: (err) => {
        console.error('Erreur lors du chargement du LCN:', err);
        this.loading = false;
        this.location.back();
      }
    });
  }
  cancel(): void {
    this.location.back();
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

  saveLcn(): void {
    if (this.lcnForm.invalid) {
      this.lcnForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs du formulaire.' });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête en utilisant getRawValue() pour inclure les champs désactivés (beneficiaire, type)
    const raw = this.lcnForm.getRawValue();
    const requestDto = { ...raw, dateEmission: toLocalDateString(raw.dateEmission)!, dateEcheance: toLocalDateString(raw.dateEcheance)! };


    let operation$: Observable<LcnResponseDTO>;
    let successMessage: string;

    if (this.isEditMode && this.lcnId) {
      operation$ = this.lcnService.updateLcn(this.lcnId, requestDto);
      successMessage = 'Lcn modifié avec succès !';
    } else {
      operation$ = this.lcnService.createLcn(requestDto);
      successMessage = 'Lcn ajouté avec succès !';
    }

    operation$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: successMessage });
        this.location.back();
      },
      error: (err) => {
        console.error('Erreur lors de l\'enregistrement du LCN:', err);
        this.loading = false;
      }
    });
  }
}
