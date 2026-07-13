import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { InputNumberModule } from 'primeng/inputnumber';
import { Decimal } from 'decimal.js';

import { MarcheRequestDTO } from '../../dtos/marche-request.dto';
import { MarcheResponseDTO } from '../../dtos/marche-response.dto';
import {MarcheService} from "../../service/marche-service.service";
import {DropdownModule} from "primeng/dropdown";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';


@Component({
  selector: 'app-marches-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    CalendarModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    RouterLink,
    ConfirmDialogModule,
    BreadcrumbModule,
    InputNumberModule,
    DropdownModule
  ],
  templateUrl: './marches-form.component.html',
  styleUrl: './marches-form.component.scss',
  providers: [
    MessageService,
    ConfirmationService
  ]
})
export class MarchesFormComponent implements OnInit {

  marcheForm!: FormGroup;
  originalMarcheResponse: MarcheResponseDTO | null = null;
  loading: boolean = false;
  isEditMode: boolean = false;
  items: MenuItem[] | undefined;

  banques: { id: number; nom: string }[] = [];
  loadingBanques: boolean = false;

  private readonly fb = inject(FormBuilder);
  private readonly marcheService = inject(MarcheService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly route = inject(ActivatedRoute);
  private readonly banqueService = inject(BanqueService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.initializeForm();
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-file-o', route: '/portail/marches', label: 'Liste des marchés' }
    ];

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.loadMarche(Number(id));
      } else {
        this.isEditMode = false;
      }
    });
  }

  initializeForm(): void {
    this.marcheForm = this.fb.group({
      numero: ['',Validators.required],
      nom: ['', Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
      maitreDouvrage: ['', Validators.required],
      dateDebutDesTravaux: [''],
      dateDAchievementDesTravaux: [''],
      banqueId: [null, Validators.required]
    });
  }

  loadMarche(id: number): void {
    this.loading = true;
    this.marcheService.getMarcheById(id).subscribe({
      next: (data: MarcheResponseDTO) => {
        this.originalMarcheResponse = data;

        this.marcheForm.patchValue({
          numero: data.numero,
          nom: data.nom,
          montant: new Decimal(data.montant),
          maitreDouvrage: data.maitreDouvrage,
          dateDebutDesTravaux: data.dateDebutDesTravaux ? parseLocalDate(data.dateDebutDesTravaux) : null,
          dateDAchievementDesTravaux: data.dateDAchievementDesTravaux ? parseLocalDate(data.dateDAchievementDesTravaux) : null,
          banqueId: data.banqueId
        });

        this.loading = false;
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Marché chargé avec succès.', life: 1000 });
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/portail/marches']);
      }
    });
  }

  loadBanques(event: any) {
    this.loadingBanques = true;

    const page = event.first / event.rows || 0;
    const size = event.rows || 10;

    const criteria = { nom: event.filter }; // filtre par nom si tapé

    this.banqueService.getAllBanques(criteria, page, size).subscribe({
      next: (data) => {
        this.banques = data.content.map(b => ({ id: b.id, nom: b.nom }));
        this.loadingBanques = false;
      },
      error: () => {
        this.loadingBanques = false;
      }
    });
  }
  // Helper for easy access to form controls in the template
  get f(): { [key: string]: AbstractControl } {
    return this.marcheForm.controls;
  }

  saveMarche(): void {
    this.marcheForm.markAllAsTouched();
    if (this.marcheForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce marché ?' : 'Êtes-vous sûr de vouloir créer ce marché ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveMarche();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveMarche(): void {
    this.loading = true;
    const formValue = this.marcheForm.getRawValue();

    const marcheRequest: MarcheRequestDTO = {
      numero: formValue.numero,
      nom: formValue.nom,
      montant: formValue.montant,
      maitreDouvrage: formValue.maitreDouvrage,
      dateDebutDesTravaux: toLocalDateString(formValue.dateDebutDesTravaux) ?? undefined,
      dateDAchievementDesTravaux: toLocalDateString(formValue.dateDAchievementDesTravaux) ?? undefined,
      banqueId: formValue.banqueId
    };


    if (this.isEditMode && this.originalMarcheResponse?.id) {
      this.marcheService.updateMarche(this.originalMarcheResponse.id, marcheRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Marché mis à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/marches']);
          },
          error: () => {
            this.loading = false;
          }
        });
    } else { // Create mode
      this.marcheService.createMarche(marcheRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Marché créé avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/marches']);
          },
          error: () => {
            this.loading = false;
          }
        });
    }
  }
}
