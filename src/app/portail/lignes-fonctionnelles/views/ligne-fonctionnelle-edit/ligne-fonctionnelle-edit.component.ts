import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidatorFn, FormArray} from '@angular/forms';
import {ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive} from '@angular/router';

import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import {MenuItem, MessageService} from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';

import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { LigneFonctionnelleRequest } from "../../dtos/LigneFonctionnelleRequest";
import { LigneFonctionnelleResponse } from '../../dtos/LigneFonctionnelleResponse';
import {NatureLigneFonctionnelleService} from "../../services/nature-ligne-fonctionnelle.service";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {TooltipModule} from "primeng/tooltip";
import {SousLigneResponseDto} from "../../dtos/SousLigneResponseDto";
import {SousLigneRequestDto} from "../../dtos/SousLigneRequestDto";

const CAUTION_ADMINISTRATIVE_ID = 16;


@Component({
  selector: 'app-ligne-fonctionnelle-edit',
  templateUrl: './ligne-fonctionnelle-edit.component.html',
  styleUrls: ['./ligne-fonctionnelle-edit.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NgIf,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    CalendarModule,
    BreadcrumbModule,
    RouterLink,
    RouterLinkActive,
    TooltipModule,
    DialogModule,
    TableModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class LigneFonctionnelleEditComponent implements OnInit {


  totalSousLignesPlafond: number = 0;
  ligneFonctionnelleForm!: FormGroup;
  isEditMode: boolean = false;
  ligneFonctionnelleId: number | null = null;
  ligneFonctionnelle?: LigneFonctionnelleResponse ;
  natureLigneOptions: { label: string; value: number }[] = [];
  banquesOptions: { label: string; value: number }[] = [];
  loading: boolean = false;
  items: MenuItem[] | undefined;


  displaySousLigneDialog: boolean = false;
  sousLigneForm!: FormGroup;
  sousLigneEditMode: boolean = false;
  currentSousLigneIndex: number | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) { }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },{ icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label:"Les Lignes fonctionnelles " } ]
    this.initForm()
    this.loadBanquesForDropdown();
    this.loadNatureLigneOptionsForDropdown();
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.ligneFonctionnelleId = Number(id);
        this.loadLigneFonctionnelle(this.ligneFonctionnelleId);
      } else {
        this.isEditMode = false;
        this.ligneFonctionnelleId = null;
        this.ligneFonctionnelleForm.get('dateDepart')?.updateValueAndValidity();
      }
    });
  }

  initForm(){
    this.ligneFonctionnelleForm = this.fb.group(
      {
        banqueId: [null, Validators.required],
        natureLigneFonctionnelleId: [null, Validators.required],
        plafond: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)],],
        taux: [null, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+(\.\d{1,2})?$/)],],
        dateDepart: [null, [Validators.required]],
        dateFin: [null, Validators.required],
        sousLignes: this.fb.array([], [this.sousLigneNatureUniquenessValidator()])
      },
      { validators: [this.dateFinAfterDateDepartValidator(),this.plafondSousLignesValidator()]}
    );

    // Formulaire dédié pour la modale des sous-lignes
    this.sousLigneForm = this.fb.group({
      id: [null],
      natureLigneFonctionnelleId: [null, Validators.required],
      plafond: [null, [Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]
      ],
    });

    this.setupHierarchicalListeners();
  }


  /** Ouvre le dialogue en mode ajout. */
  openAddSousLigneDialog(): void {
    this.sousLigneEditMode = false;
    this.currentSousLigneIndex = null;
    this.sousLigneForm.reset();
    this.displaySousLigneDialog = true;
  }

  /** Ouvre le dialogue en mode édition. */
  openEditSousLigneDialog(index: number): void {
    this.sousLigneEditMode = true;
    this.currentSousLigneIndex = index;
    // PatchValue avec la valeur du contrôle existant dans le FormArray
    const sousLigneControl = this.sousLignesArray.at(index);
    this.sousLigneForm.patchValue(sousLigneControl.value);
    this.displaySousLigneDialog = true;
  }

  /** Action lors de la fermeture du dialogue. */
  onSousLigneDialogHide(): void {
    this.sousLigneForm.reset();
    this.sousLigneEditMode = false;
    this.currentSousLigneIndex = null;
  }

  /** Sauvegarde la sous-ligne (ajout ou modification) dans le FormArray. */
  saveSousLigne(): void {
    this.sousLigneForm.markAllAsTouched();

    if (this.sousLigneForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire de la sous-ligne.', life: 3000 });
      return;
    }

    const sousLigneValue = this.sousLigneForm.value;

    if (this.sousLigneEditMode && this.currentSousLigneIndex !== null) {
      // Mode Édition : Mettre à jour le contrôle existant dans le FormArray
      this.sousLignesArray.at(this.currentSousLigneIndex).patchValue(sousLigneValue);
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Sous-ligne modifiée.', life: 1500 });
    } else {
      // Mode Ajout : Ajouter un nouveau FormGroup au FormArray
      const newSousLigneGroup = this.createSousLigneFormGroup(sousLigneValue); // Utiliser la méthode de création
      this.sousLignesArray.push(newSousLigneGroup);
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Sous-ligne ajoutée.', life: 1500 });
    }

    // Déclencher la validation globale (y compris l'unicité) et recalculer le total
    this.ligneFonctionnelleForm.updateValueAndValidity();
    this.calculateTotalSousLignesPlafond();

    // Afficher un message d'erreur si la validation d'unicité a échoué après l'opération
    if(this.sousLignesArray.hasError('duplicateSousLigneNature')){
      this.messageService.add({ severity: 'error', summary: 'Doublon Détecté', detail: 'Cette nature de sous-ligne existe déjà. Veuillez la modifier ou la supprimer.', life: 5000 });
    }

    this.displaySousLigneDialog = false; // Fermer la modale
  }

  /** Retourne le label d'une nature à partir de son ID (pour l'affichage dans le tableau). */
  getNatureLabel(id: number | null): string {
    const nature = this.natureLigneOptions.find(n => n.value === id);
    return nature ? nature.label : 'Nature Inconnue';
  }


  // Méthodes de chargement existantes
  loadNatureLigneOptionsForDropdown(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {

        this.natureLigneOptions = natures.map(nature => ({
          label: nature.label,
          value: nature.id!
        }));
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des natures de ligne.',
          life: 3000
        });
        console.error('Error loading natures:', err);
      }
    });
  }

  loadBanquesForDropdown(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .subscribe({
        next: (pageResponse) => {
          this.banquesOptions = pageResponse.content.map(banque => ({ label: banque.nom, value: banque.id! }));
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques.', life: 3000 });
          console.error('Error loading banks:', err);
        }
      });
  }

  // Getters et méthodes du FormArray
  get sousLignesArray(): FormArray {
    return this.ligneFonctionnelleForm.get('sousLignes') as FormArray;
  }


  createSousLigneFormGroup(sousLigne?: SousLigneResponseDto | SousLigneRequestDto): FormGroup {
    let initialId = null;

    if (sousLigne && 'id' in sousLigne) {
      initialId = sousLigne.id || null;
    }

    // Plafond est facultatif pour la sous-ligne, mais doit être valide s'il est présent.
    return this.fb.group({
      id: [initialId],
      plafond: [
        sousLigne?.plafond ?? null,
        [Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]
      ],
      natureLigneFonctionnelleId: [
        sousLigne?.natureLigneFonctionnelleId ?? null,
        Validators.required
      ]
    });
  }

  removeSousLigne(index: number): void {
    this.sousLignesArray.removeAt(index);
    // Important: forcer la validation après suppression
    this.ligneFonctionnelleForm.get('plafond')?.updateValueAndValidity();
    this.calculateTotalSousLignesPlafond();
    this.ligneFonctionnelleForm.updateValueAndValidity(); // Réexécute le validateur d'unicité
  }

  get isCautionAdministrativeSelected(): boolean {
    const selectedId = this.ligneFonctionnelleForm.get('natureLigneFonctionnelleId')?.value;
    return selectedId === CAUTION_ADMINISTRATIVE_ID;
  }

  loadLigneFonctionnelle(id: number): void {
    this.loading = true;
    this.ligneFonctionnelleService.getLigneFonctionnelleById(id).subscribe({
      next: (ligne: LigneFonctionnelleResponse) => {
        this.ligneFonctionnelleForm.patchValue({
          banqueId: ligne.banqueId,
          natureLigneFonctionnelleId: ligne.natureLigneFonctionnelleId,
          plafond: ligne.plafond,
          taux: ligne.taux,
          dateDepart: ligne.dateDepart ? new Date(ligne.dateDepart) : null,
          dateFin: ligne.dateFin ? new Date(ligne.dateFin) : null,
        });

        if (ligne.estLigneMere && ligne.sousLignes && ligne.sousLignes.length > 0) {
          this.sousLignesArray.clear(); // Vider avant de populer
          ligne.sousLignes.forEach(sousLigne => {
            this.sousLignesArray.push(this.createSousLigneFormGroup(sousLigne));
          });
          this.calculateTotalSousLignesPlafond();
        }

        this.ligneFonctionnelle = ligne;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la ligne fonctionnelle:', error);
        const errorMessage = error.error?.message || 'Impossible de charger la ligne fonctionnelle.';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
        this.loading = false;
        this.router.navigate(['/portail/lignes-fonctionnelles']);
      }
    });
  }

  onSubmit(): void {
    this.ligneFonctionnelleForm.markAllAsTouched();
    // Re-run validation on the form group level for cross-field validators
    this.ligneFonctionnelleForm.updateValueAndValidity();

    if (this.ligneFonctionnelleForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette ligne fonctionnelle ?' : 'Êtes-vous sûr de vouloir créer cette ligne fonctionnelle ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveLigneFonctionnelle();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveLigneFonctionnelle(): void {
    this.loading = true;

    // 1. Récupérer la valeur brute du formulaire (y compris le FormArray 'sousLignes')
    const formValue = this.ligneFonctionnelleForm.value;

    // 2. Déterminer les sous-lignes à envoyer :
    //    - On les inclut UNIQUEMENT si la nature est "Caution Administrative" ET qu'il y en a dans le FormArray.
    //    - Sinon, on envoie un tableau vide.
    const sousLignesToSend: SousLigneRequestDto[] = this.isCautionAdministrativeSelected && formValue.sousLignes.length > 0
      ? formValue.sousLignes
      : [];

    // 3. Construction de la requête principale
    const ligneRequest: LigneFonctionnelleRequest = {
      banqueId: formValue.banqueId,
      natureLigneFonctionnelleId: formValue.natureLigneFonctionnelleId,
      plafond: formValue.plafond,
      taux: formValue.taux,
      dateDepart: formValue.dateDepart,
      dateFin: formValue.dateFin,
      // Les sous-lignes sont incluses ici pour le POST (Création en cascade)
      sousLignes: sousLignesToSend
    };

    if (this.isEditMode && this.ligneFonctionnelleId) {
      // --- SCÉNARIO MISE À JOUR (PUT /lignes-fonctionnelles/{id}) ---

      // IMPORTANT: Pour l'édition de la Ligne Mère/Indépendante, on met à jour SEULEMENT
      // les attributs principaux (taux, dates, plafond) et on s'assure que le tableau sousLignes
      // est vide. La gestion des enfants se fait par l'endpoint dédié `manageSousLignes`.
      const mereUpdateRequest: LigneFonctionnelleRequest = { ...ligneRequest, sousLignes: [] };

      this.ligneFonctionnelleService.updateLigneFonctionnelle(this.ligneFonctionnelleId, mereUpdateRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle mise à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
          },
          error: (error) => {
            console.error('Erreur lors de la mise à jour de la ligne fonctionnelle:', error);
            const errorMessage = error.error?.message || 'Échec de la mise à jour de la ligne fonctionnelle.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    } else {
      // --- SCÉNARIO CRÉATION (POST /lignes-fonctionnelles) ---
      this.ligneFonctionnelleService.createLigneFonctionnelle(ligneRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle créée avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
          },
          error: (error) => {
            console.error('Erreur lors de la création de la ligne fonctionnelle:', error);
            const errorMessage = error.error?.message || 'Échec de la création de la ligne fonctionnelle.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    }
  }


  // Écouteurs pour recalculer le total et valider
  setupHierarchicalListeners(): void {
    // Écoute les changements dans le FormArray
    this.sousLignesArray.valueChanges.subscribe(() => {
      this.calculateTotalSousLignesPlafond();
      this.ligneFonctionnelleForm.updateValueAndValidity(); // Réexécute le validateur de groupe
    });
    // Écoute les changements dans le plafond mere
    this.ligneFonctionnelleForm.get('plafond')?.valueChanges.subscribe(() => {
      this.ligneFonctionnelleForm.updateValueAndValidity(); // Réexécute le validateur de groupe
    });

    // Gérer la désactivation du FormArray si la Nature n'est pas "Caution Administrative"
    this.ligneFonctionnelleForm.get('natureLigneFonctionnelleId')?.valueChanges.subscribe((value) => {
      if (value !== CAUTION_ADMINISTRATIVE_ID) {
        // Si la nature change, vider et nettoyer le FormArray pour éviter l'envoi de données
        this.sousLignesArray.clear();
      }
      // Force la réévaluation des erreurs de validation croisée au changement de nature.
      this.ligneFonctionnelleForm.updateValueAndValidity();
    });
  }

  // Calcule la somme pour l'affichage (UX) et la validation
  calculateTotalSousLignesPlafond(): void {
    this.totalSousLignesPlafond = this.sousLignesArray.controls.reduce((sum, control) => {
      const plafond = control.get('plafond')?.value;
      // S'assurer que le plafond est un nombre valide (> 0) avant l'addition
      return sum + (typeof plafond === 'number' && plafond > 0 ? plafond : 0);
    }, 0);
  }



  onCancel(): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
  }

  plafondSousLignesValidator(): ValidatorFn {
    return (formGroup: AbstractControl): { [key: string]: any } | null => {
      const selectedNatureId = formGroup.get('natureLigneFonctionnelleId')?.value;
      const plafondMereControl = formGroup.get('plafond');
      const sousLignesArray = formGroup.get('sousLignes') as FormArray;

      // La validation n'a de sens que si la nature est 'Caution Administrative' et qu'il y a des enfants
      if (selectedNatureId !== CAUTION_ADMINISTRATIVE_ID || sousLignesArray.length === 0 || !plafondMereControl || !plafondMereControl.value) {
        // Nettoyer l'erreur si la condition n'est plus remplie
        if (plafondMereControl && plafondMereControl.hasError('plafondExceeded')) {
          const errors = { ...plafondMereControl.errors };
          delete errors['plafondExceeded'];
          plafondMereControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }
        return null;
      }

      const plafondMere = plafondMereControl.value;

      // Utiliser totalSousLignesPlafond que nous maintenons à jour
      const totalPlafondFilles = this.totalSousLignesPlafond;

      if (totalPlafondFilles > plafondMere) {
        // Définir l'erreur sur le champ plafond de la Mère
        plafondMereControl.setErrors({ ...plafondMereControl.errors, 'plafondExceeded': true });
        return { 'totalPlafondExceeded': true }; // Erreur au niveau du Formulaire
      } else {
        // Nettoyer l'erreur 'plafondExceeded'
        if (plafondMereControl.hasError('plafondExceeded')) {
          const errors = { ...plafondMereControl.errors };
          delete errors['plafondExceeded'];
          plafondMereControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }
      }
      return null;
    };
  }
  //  dateFin must be after dateDepart
  dateFinAfterDateDepartValidator(): ValidatorFn {
    return (formGroup: AbstractControl): { [key: string]: any } | null => {
      const dateDepartControl = formGroup.get('dateDepart');
      const dateFinControl = formGroup.get('dateFin');

      if (!dateDepartControl || !dateFinControl || !dateDepartControl.value || !dateFinControl.value) {
        return null; // Don't validate if dates are not yet set
      }

      const dateDepart = dateDepartControl.value as Date;
      const dateFin = dateFinControl.value as Date;

      if (dateFin < dateDepart) {
        dateFinControl.setErrors({ ...dateFinControl.errors, 'minDate': true }); // Set error on dateFin
      } else {
        // This is crucial because errors are additive in Reactive Forms
        if (dateFinControl.hasError('minDate')) {
          const errors = { ...dateFinControl.errors }; // Get existing errors
          delete errors['minDate']; // Remove the specific error
          dateFinControl.setErrors(Object.keys(errors).length > 0 ? errors : null); // Set remaining errors or null
        }
      }
      return null;
    };
  }
  /**
   * Validateur personnalisé pour vérifier que les natures de ligne sont uniques
   * à l'intérieur du FormArray des sous-lignes.
   */
  sousLigneNatureUniquenessValidator(): ValidatorFn {
    return (formArray: AbstractControl): { [key: string]: any } | null => {
      if (!(formArray instanceof FormArray)) {
        return null;
      }

      const natures: number[] = [];
      let hasDuplicate = false;

      // Parcourir tous les FormGroups du FormArray
      for (const control of formArray.controls) {
        const natureId = control.get('natureLigneFonctionnelleId')?.value;

        // Réinitialiser l'erreur spécifique à la nature dupliquée sur chaque contrôle
        const errors = control.get('natureLigneFonctionnelleId')?.errors;
        if (errors && errors['duplicateNature']) {
          const newErrors = { ...errors };
          delete newErrors['duplicateNature'];
          control.get('natureLigneFonctionnelleId')?.setErrors(Object.keys(newErrors).length > 0 ? newErrors : null);
        }

        if (natureId !== null && natureId !== undefined) {
          if (natures.includes(natureId)) {
            hasDuplicate = true;
            // Définir l'erreur sur le contrôle dupliqué
            control.get('natureLigneFonctionnelleId')?.setErrors({ ...control.get('natureLigneFonctionnelleId')?.errors, 'duplicateNature': true });
          } else {
            natures.push(natureId);
          }
        }
      }

      // Retourner l'erreur globale au niveau du FormArray
      return hasDuplicate ? { 'duplicateSousLigneNature': true } : null;
    };
  }
  get f() {
    return this.ligneFonctionnelleForm.controls;
  }
}
