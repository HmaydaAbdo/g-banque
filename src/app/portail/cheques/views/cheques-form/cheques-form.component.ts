import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { RippleModule } from "primeng/ripple";

// Services et DTOs
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueDto } from '../../../cmt/dtos/BanqueDto';
import { ChequeResponseDto } from '../../dtos/ChequeResponseDto';
import {Type} from "../../../Virements/Type";
import {ChequeService} from "../../services/ChequeService";

@Component({
  selector: 'app-cheques-form',
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
  templateUrl: './cheques-form.component.html',
  styleUrl: './cheques-form.component.scss'
})
export class ChequesFormComponent implements OnInit {

  chequeForm!: FormGroup;
  isEditMode: boolean = false;
  chequeId: number | null = null;
  loading: boolean = false;
  banques: BanqueDto[] = [];


  chequeType: Type = Type.ENCAISSEMENT;
  beneficiaireFixe: string = "STAPORT";

  items: MenuItem[] = [
    { label: 'Accueil', routerLink: '/portail' },
    { label: 'Chèques ( Encaissements )', routerLink: '/portail/Cheques/encaissements' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private banqueService: BanqueService,
    private chequeService: ChequeService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.checkModeAndLoadData();
  }

  // Getter pour simplifier l'accès aux contrôles du formulaire
  get f() {
    return this.chequeForm.controls;
  }

  private initForm(cheque?: ChequeResponseDto): void {

    this.chequeForm = this.fb.group({
      numero: [cheque ? cheque.numero : null, Validators.required],
      banqueId: [cheque ? cheque.banqueId : null, Validators.required],
      dateEmission: [cheque ? new Date(cheque.dateEmission) : null, Validators.required],
      dateEcheance: [cheque ? new Date(cheque.dateEcheance) : null], // Optionnel
      montant: [cheque ? cheque.montant : null, [Validators.required, Validators.min(0.01)]],
      emetteur: [cheque ? cheque.emetteur : '', Validators.required],
      beneficiaire: [{ value: this.beneficiaireFixe, disabled: true }, Validators.required],
      type: [{ value: this.chequeType, disabled: true }, Validators.required],
    });
  }


  private checkModeAndLoadData(): void {
    this.route.paramMap.pipe(
      // 1. Vérifier si un ID est présent dans l'URL et charger le chèque si nécessaire
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          this.chequeId = +id;
          this.isEditMode = true;
          this.loading = true;
          return this.chequeService.getChequeById(this.chequeId);
        } else {
          this.isEditMode = false;
          return of(null);
        }
      }),
      // 2. Si c'est le mode édition, charger explicitement les détails de la Banque
      switchMap(chequeResponse => {
        if (chequeResponse && chequeResponse.banqueId) {
          // Charger la banque correspondant à l'ID pour l'ajouter aux options de la dropdown
          return this.banqueService.getBanqueById(chequeResponse.banqueId).pipe(
            switchMap(banque => {
              // Ajouter la banque aux options 'this.banques' pour qu'elle soit sélectionnable
              // et affichée dans le p-dropdown (nécessaire avec le lazy loading)
              this.banques = [banque];
              return of(chequeResponse);
            })
          );
        }
        return of(chequeResponse);
      })
    ).subscribe({
      next: (chequeResponse) => {
        // 3. Initialiser le formulaire une fois toutes les données chargées
        this.initForm(chequeResponse || undefined);
        this.loading = false;

      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement du chèque.' });
        this.loading = false;
        // Redirection en cas d'erreur de chargement
        this.router.navigate(['/portail/Cheques/encaissements']);
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

  saveCheque(): void {
    if (this.chequeForm.invalid) {
      this.chequeForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs du formulaire.' });
      return;
    }

    this.loading = true;

    // Construire le DTO de requête en utilisant getRawValue() pour inclure les champs désactivés (beneficiaire, type)
    const requestDto = this.chequeForm.getRawValue();


    let operation$: Observable<ChequeResponseDto>;
    let successMessage: string;

    if (this.isEditMode && this.chequeId) {
      operation$ = this.chequeService.updateCheque(this.chequeId, requestDto);
      successMessage = 'Chèque modifié avec succès !';
    } else {
      operation$ = this.chequeService.createCheque(requestDto);
      successMessage = 'Chèque ajouté avec succès !';
    }

    operation$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: successMessage });
        // Rediriger après un petit délai
         this.router.navigate(['/portail/Cheques/encaissements'])
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message || 'Échec de l\'opération.' });
        this.loading = false;
      }
    });
  }
}
