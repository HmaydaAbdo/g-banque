import {Component, OnInit} from '@angular/core';
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ButtonDirective} from "primeng/button";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DropdownModule} from "primeng/dropdown";
import {InputNumberModule} from "primeng/inputnumber";
import {InputTextModule} from "primeng/inputtext";
import {NgClass, NgIf} from "@angular/common";
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
import {ChequeService} from "../../../cheques/services/ChequeService";
import {ChequeResponseDto} from "../../../cheques/dtos/ChequeResponseDto";
import {Observable, of, switchMap} from "rxjs";
import {LcnService} from "../../service/LcnService";
import {LcnResponseDTO} from "../../dtos/LcnResponseDTO";

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
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ProgressSpinnerModule,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    ToastModule,
    NgClass
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
    private messageService: MessageService
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
      dateEmission: [lcn ? new Date(lcn.dateEmission) : null, Validators.required],
      dateEcheance: [lcn ? new Date(lcn.dateEcheance) : null,Validators.required],
      montant: [lcn ? lcn.montant : null, [Validators.required, Validators.min(0.01)]],
      emetteur: [lcn ? lcn.emetteur : '', Validators.required],
      beneficiaire: [{ value: this.beneficiaireFixe, disabled: true }, Validators.required],
      type: [{ value: this.lcnType, disabled: true }, Validators.required],
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
      // 2. Si c'est le mode édition, charger explicitement les détails de la Banque
      switchMap(lcnResponse => {
        if (lcnResponse && lcnResponse.banqueId) {
          // Charger la banque correspondant à l'ID pour l'ajouter aux options de la dropdown
          return this.banqueService.getBanqueById(lcnResponse.banqueId).pipe(
            switchMap(banque => {
              // Ajouter la banque aux options 'this.banques' pour qu'elle soit sélectionnable
              // et affichée dans le p-dropdown (nécessaire avec le lazy loading)
              this.banques = [banque];
              return of(lcnResponse);
            })
          );
        }
        return of(lcnResponse);
      })
    ).subscribe({
      next: (lcnResponse) => {
        // 3. Initialiser le formulaire une fois toutes les données chargées
        this.initForm(lcnResponse || undefined);
        this.loading = false;

        // Mettre à jour le breadcrumb
        this.items.push({
          label: this.isEditMode ? 'Modifier' : 'Ajouter',
        });
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement du lcn.' });
        this.loading = false;
        // Redirection en cas d'erreur de chargement
        this.router.navigate(['/portail/LCN/encaissements']);
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

  saveLcn(): void {
    if (this.lcnForm.invalid) {
      this.lcnForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs du formulaire.' });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête en utilisant getRawValue() pour inclure les champs désactivés (beneficiaire, type)
    const requestDto = this.lcnForm.getRawValue();


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
        // Rediriger après un petit délai
        this.router.navigate(['/portail/LCN/encaissements'])
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message || 'Échec de l\'opération.' });
        this.loading = false;
      }
    });
  }
}
