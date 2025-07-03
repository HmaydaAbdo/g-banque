import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap } from '@angular/router';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';

// Application-specific imports
import { NatureLigneFonctionnelleService } from '../../services/nature-ligne-fonctionnelle.service';
import { NatureLigneFonctionnelleRequest } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleRequest';
import { NatureLigneFonctionnelleResponse } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse';

@Component({
  selector: 'app-nature-ligne-fonctionnelle-edit',
  templateUrl: './nature-ligne-fonctionnelle-edit.component.html',
  styleUrls: ['./nature-ligne-fonctionnelle-edit.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class NatureLigneFonctionnelleEditComponent implements OnInit {
  natureLigneForm: FormGroup;
  isEditMode: boolean = false;
  natureLigneId: number | null = null;
  loading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private natureService: NatureLigneFonctionnelleService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {
    this.natureLigneForm = this.fb.group({
      label: ['', [Validators.required, Validators.pattern(/^[A-Za-zÀ-ÿ\s]*$/)]],
      description: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.natureLigneId = Number(id);
        this.loadNatureLigne(this.natureLigneId);
      } else {
        this.isEditMode = false;
        this.natureLigneId = null;
      }
    });
  }

  loadNatureLigne(id: number): void {
    this.loading = true;
    this.natureService.getNatureLigneFonctionnelleById(id).subscribe({
      next: (nature: NatureLigneFonctionnelleResponse) => {
        this.natureLigneForm.patchValue(nature);
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la nature de ligne fonctionnelle:', error);
        const errorMessage = error.error?.message || 'Impossible de charger la nature de ligne fonctionnelle.';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
        this.loading = false;
        this.router.navigate(['/portail/natures-fonctionnelles']);
      }
    });
  }

  get f() {
    return this.natureLigneForm.controls;
  }

  onSubmit(): void {
    this.natureLigneForm.markAllAsTouched();
    if (this.natureLigneForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette nature de ligne fonctionnelle ?' : 'Êtes-vous sûr de vouloir créer cette nature de ligne fonctionnelle ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveNatureLigne();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveNatureLigne(): void {
    this.loading = true;
    const natureRequest: NatureLigneFonctionnelleRequest = this.natureLigneForm.value;

    if (this.isEditMode && this.natureLigneId) {
      this.natureService.updateNatureLigneFonctionnelle(this.natureLigneId, natureRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Nature de ligne fonctionnelle mise à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/natures-fonctionnelles']);
          },
          error: (error) => {
            console.error('Erreur lors de la mise à jour de la nature de ligne fonctionnelle:', error);
            const errorMessage = error.error?.message || 'Échec de la mise à jour de la nature de ligne fonctionnelle.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    } else {
      this.natureService.createNatureLigneFonctionnelle(natureRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Nature de ligne fonctionnelle créée avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/natures-fonctionnelles']);
          },
          error: (error) => {
            console.error('Erreur lors de la création de la nature de ligne fonctionnelle:', error);
            const errorMessage = error.error?.message || 'Échec de la création de la nature de ligne fonctionnelle.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/natures-fonctionnelles']);
  }
}
