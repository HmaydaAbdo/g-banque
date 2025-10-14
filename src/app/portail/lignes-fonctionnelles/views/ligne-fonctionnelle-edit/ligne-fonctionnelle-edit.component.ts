import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidatorFn } from '@angular/forms';
import {ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive} from '@angular/router'; // Corrected import for ActivatedRoute and added ParamMap


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
import { LigneFonctionnelleService } from "../../services/LigneFonctionnelleService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { LigneFonctionnelleRequest } from "../../dtos/LigneFonctionnelleRequest";
import { LigneFonctionnelleResponse } from '../../dtos/LigneFonctionnelleResponse';
import {NatureLigneFonctionnelleService} from "../../services/nature-ligne-fonctionnelle.service";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {TooltipModule} from "primeng/tooltip";

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
    TooltipModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class LigneFonctionnelleEditComponent implements OnInit {

  ligneFonctionnelleForm!: FormGroup;
  isEditMode: boolean = false;
  ligneFonctionnelleId: number | null = null;
  ligneFonctionnelle?: LigneFonctionnelleResponse ;
  natureLigneOptions: { label: string; value: number }[] = [];
  banquesOptions: { label: string; value: number }[] = [];
  loading: boolean = false;

  items: MenuItem[] | undefined;
  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private ligneFonctionnelleService: LigneFonctionnelleService,
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService, // NEW: Inject NatureLigneFonctionnelleService
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {

  }

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },{ icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label:"Les Lignes fonctionnelles Encours" } ]
    this.initForm()
    this.loadBanquesForDropdown();
    this.loadNatureLigneOptionsForDropdown(); // This will now fetch from backend
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.ligneFonctionnelleId = Number(id);
        this.loadLigneFonctionnelle(this.ligneFonctionnelleId);
      } else {
        this.isEditMode = false;
        this.ligneFonctionnelleId = null;
        // If in create mode, ensure date validation checks against current date
        this.ligneFonctionnelleForm.get('dateDepart')?.updateValueAndValidity();
      }
    });
  }

  initForm(){
    this.ligneFonctionnelleForm = this.fb.group({
      banqueId: [null, Validators.required],
      // Changed: form control name from 'nature' to 'natureLigneFonctionnelleId'
      natureLigneFonctionnelleId: [null, Validators.required],
      plafond: [
        null,
        [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)],
      ],
      taux: [
        null,
        [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+(\.\d{1,2})?$/)],
      ],

      dateDepart: [null, [Validators.required]],
      dateFin: [null, Validators.required]
    }, {
      validators: this.dateFinAfterDateDepartValidator()
    });
  }



  // Custom validator for cross-field validation: dateFin must be after dateDepart

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
        return { 'dateMismatch': true }; // Optional: set error on the form group
      } else {
        // If valid, clear the specific 'minDate' error from dateFinControl
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

  loadNatureLigneOptionsForDropdown(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {

        const naturesFiltrees = natures.filter(nature =>
          !['Préfinancement', 'Droit constaté', 'Factoring'].includes(nature.label)
        );


        this.natureLigneOptions = naturesFiltrees.map(nature => ({
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

  get f() {
    return this.ligneFonctionnelleForm.controls;
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
    // The service handles converting Date objects to ISO strings
    const ligneRequest: LigneFonctionnelleRequest = this.ligneFonctionnelleForm.value;

    if (this.isEditMode && this.ligneFonctionnelleId) {
      this.ligneFonctionnelleService.updateLigneFonctionnelle(this.ligneFonctionnelleId, ligneRequest)
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

  desactiver(): void {
    if (this.ligneFonctionnelle == null) {
      console.log("ligneFonctionnelle est null")
    } else {
      const ligne = this.ligneFonctionnelle;
      this.confirmationService.confirm({
        message: `Êtes-vous sûr de vouloir désactiver la ligne fonctionnelle "${ligne.natureLigneFonctionnelleLabel}" avec  "${ligne.banqueNom}" ?`,
        header: 'Confirmation de Désactivation',
        icon: 'pi pi-exclamation-triangle',
        accept: () => {
          this.loading = true;
          this.ligneFonctionnelleService.deactivateLigneFonctionnelle(ligne.id!)
            .subscribe({
              next: () => {
                this.messageService.add({
                  severity: 'success',
                  summary: 'Succès',
                  detail: 'Ligne Fonctionnelle désactivées avec succès.',
                  life: 3000
                });
                this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
              },
              error: (error) => {
                console.error('Erreur lors de la désactivation de la ligne fonctionnelle:', error);
                const errorMessage = error.error?.message;
                this.messageService.add({severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000});
                this.loading = false;
              }
            });
        }
      });
    }

  }

  onCancel(): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-encours']);
  }
}
