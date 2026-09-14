import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {ActivatedRoute, Router, ParamMap, RouterLinkActive} from '@angular/router';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import {MenuItem, MessageService} from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { DropdownModule } from 'primeng/dropdown';

// Application-specific imports
import { NatureLigneFonctionnelleService } from '../../services/nature-ligne-fonctionnelle.service';
import { NatureLigneFonctionnelleRequest } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleRequest';
import { NatureLigneFonctionnelleResponse } from '../../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse';
import { PorteeNature, PORTEE_NATURE_LABELS } from '../../dtos/nature-ligne-fonctionnelle/PorteeNature';
import {BreadcrumbModule} from "primeng/breadcrumb";

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
    ConfirmDialogModule,
    DropdownModule,
    BreadcrumbModule,
    RouterLinkActive
  ],
  providers: [MessageService, ConfirmationService]
})
export class NatureLigneFonctionnelleEditComponent implements OnInit {
  natureLigneForm: FormGroup;
  isEditMode: boolean = false;
  natureLigneId: number | null = null;
  loading: boolean = false;
  items: MenuItem[] | undefined;

  /** Options du dropdown Portée */
  porteeOptions = Object.values(PorteeNature).map(value => ({
    label: PORTEE_NATURE_LABELS[value],
    value
  }));

  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private natureService = inject(NatureLigneFonctionnelleService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  constructor() {
    this.natureLigneForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(20)]],
      label: ['', [Validators.required]],
      description: ['', Validators.required],
      portee: [null, Validators.required],
    });
  }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-encours', label:"Les Lignes fonctionnelles" },
      { icon: 'pi pi-list', route: '/portail/natures-fonctionnelles', label:"Les Natures des Lignes fonctionnelles" },
    ]

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
        // Le code est immuable — désactivé en mode modification
        this.natureLigneForm.get('code')?.disable();
        this.loading = false;
      },
      error: () => {
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
    // getRawValue() inclut les champs disabled (ex: code en mode modification)
    const natureRequest: NatureLigneFonctionnelleRequest = this.natureLigneForm.getRawValue();

    if (this.isEditMode && this.natureLigneId) {
      this.natureService.updateNatureLigneFonctionnelle(this.natureLigneId, natureRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Nature de ligne fonctionnelle mise à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/natures-fonctionnelles']);
          },
          error: () => {
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
          error: () => {
            this.loading = false;
          }
        });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/natures-fonctionnelles']);
  }
}
