import {Component, OnInit} from '@angular/core';
import {CommonModule, NgIf} from '@angular/common';
import {AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators} from '@angular/forms';
import {ActivatedRoute, ParamMap, Router, RouterLink, RouterLinkActive} from '@angular/router';

import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {CalendarModule} from 'primeng/calendar';
import {BreadcrumbModule} from "primeng/breadcrumb";
import {TooltipModule} from "primeng/tooltip";
import {SpecificFunctionalLigneService} from "../../service/SpecificFunctionalLigneService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MarcheService} from "../../../Marches/service/marche-service.service";
import {NatureLigneFonctionnelleService} from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";
import {LigneFonctionnelleSpecifiqueRequest} from "../../dtos/LigneFonctionnelleSpecifiqueRequest";
import {LigneFonctionnelleSpecifiqueResponse} from "../../dtos/LigneFonctionnelleSpecifiqueResponse";
import {Role} from "../../../../auth/enum/Role";
import {AuthService} from "../../../../auth/services/auth.service";

@Component({
  selector: 'app-spefic-ligne-edit',
  templateUrl: './spefic-ligne-edit.component.html',
  styleUrls: ['./spefic-ligne-edit.component.scss'],
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
export class SpeficLigneEditComponent implements OnInit {

  ligneFonctionnelleSpecifiqueForm!: FormGroup;
  isEditMode: boolean = false;
  ligneFonctionnelleSpecifiqueId: number | null = null;
  ligneFonctionnelleSpecifique?: LigneFonctionnelleSpecifiqueResponse;
  natureLigneOptions: { label: string; value: number }[] = [];
  banquesOptions: { label: string; value: number }[] = [];
  marchesOptions: { label: string; value: number | null }[] = [];
  marchesTotalRecords: number = 0;
  loading: boolean = false;



  items: MenuItem[] | undefined;

  marchesScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: (event: any) => this.loadMarchesForDropdown(event)
  };

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private specificLigneService: SpecificFunctionalLigneService,
    private banqueService: BanqueService,
    private marcheService: MarcheService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" }, { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-specifiques-encours', label: "Les Lignes Fonctionnelles Spécifiques" }];
    this.initForm();
    this.loadBanquesForDropdown();

    this.loadNatureLigneOptionsForDropdown();
    this.loadMarchesForDropdown({ first: 0, last: 1000 });
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.ligneFonctionnelleSpecifiqueId = Number(id);
        this.loadLigneFonctionnelleSpecifique(this.ligneFonctionnelleSpecifiqueId);
      } else {
        this.isEditMode = false;
        this.ligneFonctionnelleSpecifiqueId = null;
      }
    });
  }

  initForm(): void {
    this.ligneFonctionnelleSpecifiqueForm = this.fb.group({
      banqueId: [null, Validators.required],
      natureLigneFonctionnelleId: [null, Validators.required],
      marcheId: [null, Validators.required],
      plafond: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)],],
      taux: [null, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+(\.\d{1,2})?$/)],],
      dateDepart: [null, [Validators.required]],
      dateFin: [null, Validators.required],
    }, {
      validators: this.dateFinAfterDateDepartValidator()
    });
  }


  dateFinAfterDateDepartValidator(): ValidatorFn {
    return (formGroup: AbstractControl): { [key: string]: any } | null => {
      const dateDepartControl = formGroup.get('dateDepart');
      const dateFinControl = formGroup.get('dateFin');

      if (!dateDepartControl || !dateFinControl || !dateDepartControl.value || !dateFinControl.value) {
        return null;
      }

      const dateDepart = dateDepartControl.value as Date;
      const dateFin = dateFinControl.value as Date;

      if (dateFin < dateDepart) {
        dateFinControl.setErrors({ ...dateFinControl.errors, 'minDate': true });
        return { 'dateMismatch': true };
      } else {
        if (dateFinControl.hasError('minDate')) {
          const errors = { ...dateFinControl.errors };
          delete errors['minDate'];
          dateFinControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }
      }
      return null;
    };
  }

  loadNatureLigneOptionsForDropdown(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {
        this.natureLigneOptions = natures.map(nature => ({
          label: nature.label,
          value: nature.id!
        }));
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des natures de ligne.', life: 3000 });
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
        }
      });
  }

  loadMarchesForDropdown(event: { first: number, last: number, filter?: string }): void {
    this.loading = true;
    const page = Math.floor(event.first / 10);
    const size = 10;
    const sort = 'nom,asc';

    this.marcheService.getAllMarchesForLazyLoad(page, size, sort).subscribe({
      next: (pageResponse) => {
        this.marchesOptions = pageResponse.content.map(marche => ({label: marche.nom, value: marche.id!}));
        this.marchesTotalRecords = pageResponse.page.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des marchés.', life: 3000 });
        this.loading = false;
      }
    });
  }

  loadLigneFonctionnelleSpecifique(id: number): void {
    this.loading = true;
    this.specificLigneService.getLigneFonctionnelleSpecifiqueById(id).subscribe({
      next: (ligne: LigneFonctionnelleSpecifiqueResponse) => {
        this.ligneFonctionnelleSpecifiqueForm.patchValue({
          banqueId: ligne.banqueId,
          natureLigneFonctionnelleId: ligne.natureLigneFonctionnelleId,
          marcheId: ligne.marcheId,
          plafond: ligne.plafond,
          taux: ligne.taux,
          dateDepart: ligne.dateDepart ? new Date(ligne.dateDepart) : null,
          dateFin: ligne.dateFin ? new Date(ligne.dateFin) : null,
        });
        this.ligneFonctionnelleSpecifique = ligne;
        this.loading = false;

      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Impossible de charger la ligne fonctionnelle spécifique.';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
        this.loading = false;
        this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
      }
    });
  }

  get f() {
    return this.ligneFonctionnelleSpecifiqueForm.controls;
  }

  onSubmit(): void {
    this.ligneFonctionnelleSpecifiqueForm.markAllAsTouched();
    this.ligneFonctionnelleSpecifiqueForm.updateValueAndValidity();

    if (this.ligneFonctionnelleSpecifiqueForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette ligne fonctionnelle spécifique ?' : 'Êtes-vous sûr de vouloir créer cette ligne fonctionnelle spécifique ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveLigneFonctionnelleSpecifique();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveLigneFonctionnelleSpecifique(): void {
    this.loading = true;
    const ligneRequest: LigneFonctionnelleSpecifiqueRequest = this.ligneFonctionnelleSpecifiqueForm.value;

    if (this.isEditMode && this.ligneFonctionnelleSpecifiqueId) {
      this.specificLigneService.updateLigneFonctionnelleSpecifique(this.ligneFonctionnelleSpecifiqueId, ligneRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle Spécifique mise à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
          },
          error: (error) => {
            const errorMessage = error.error?.message || 'Échec de la mise à jour de la ligne fonctionnelle spécifique.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    } else {
      this.specificLigneService.createLigneFonctionnelleSpecifique(ligneRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne Fonctionnelle Spécifique créée avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
          },
          error: (error) => {
            const errorMessage = error.error?.message || 'Échec de la création de la ligne fonctionnelle spécifique.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    }
  }

  desactiver(): void {
    if (this.ligneFonctionnelleSpecifique == null) {
      console.log("ligneFonctionnelleSpecifique est null");
      return;
    }
    const ligne = this.ligneFonctionnelleSpecifique;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir désactiver la ligne fonctionnelle spécifique "${ligne.natureLigneFonctionnelleLabel}" pour la banque "${ligne.banqueNom}" ?`,
      header: 'Confirmation de Désactivation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.loading = true;
        this.specificLigneService.deactivateLigneFonctionnelle(ligne.id!)
          .subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Ligne Fonctionnelle Spécifique désactivée avec succès.',
                life: 3000
              });
              this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
            },
            error: (error) => {
              const errorMessage = error.error?.message;
              this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
              this.loading = false;
            }
          });
      }
    });
  }

  canDeactivate(): boolean {
    return this.authService.hasRole(Role.ADMIN) || this.authService.hasRole(Role.SUPER_ADMIN);
  }

  onCancel(): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
  }
  private toDateOnly(dateString: string): Date | null {
    if (!dateString) return null;
    const d = new Date(dateString); // parse ISO
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()); // reset to local midnight
  }

}
