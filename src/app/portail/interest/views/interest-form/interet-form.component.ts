import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap } from '@angular/router';

// Interet specific DTOs and Service


// Shared imports
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import { Status } from '../../../prefinancements/enums/Status';

// PrimeNG imports
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';
import {InteretService} from "../../service/InteretService";
import {InteretResponseDto} from "../../dtos/InteretResponseDto";
import {InteretRequestDto} from "../../dtos/InteretRequestDto";

@Component({
  selector: 'app-interet-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    InputNumberModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    CalendarModule,
    BreadcrumbModule,
    TooltipModule,
    DatePipe,
  ],
  templateUrl: './interet-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class InteretFormComponent implements OnInit {
  interetForm!: FormGroup;
  isEditMode = false;
  interetId: number | null = null;
  loading = false;

  banquesOptions: BanqueResponse[] = [];
  statusOptions: { label: string; value: Status | null }[] = [];

  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private interetService: InteretService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupBreadcrumbs();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.interetId = Number(id);
        this.loadInteret(this.interetId);
      }
    });
  }

  initForm(): void {
    // Structure matches Commission/Agios
    this.interetForm = this.fb.group({
      banqueId: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      dateEmission: [null, Validators.required],
      observation: [null, Validators.required],
      libelle: [null, Validators.required],
    });
  }

  loadBanques(event: any): void {
    this.loading = true;
    const page = Math.floor((event.first ?? 0) / (event.rows ?? 10));
    const size = event.rows ?? 10;
    const sort = 'nom,asc';

    this.banqueService.getAllBanques({}, page, size, sort).subscribe({
      next: (res: PageResponse<BanqueResponse>) => {
        this.banquesOptions = res.content;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }



  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/interets', label: 'Intérêts' }, // Updated label and route
    ];
  }

  loadInteret(id: number): void {
    this.loading = true;
    this.interetService.getById(id).subscribe({ // Updated service call
      next: (interet: InteretResponseDto) => { // Updated DTO
        this.interetForm.patchValue({
          banqueId: interet.banqueId,
          observation: interet.observation,
          libelle: interet.libelle,
          montant: interet.montant,
          dateEmission: new Date(interet.dateEmission), // Convert string date to Date object
        });

        // Add bank to options if missing
        if (interet.banqueId && !this.banquesOptions.find(b => b.id === interet.banqueId)) {
          this.banqueService.getBanqueById(interet.banqueId).subscribe(banque => {
            this.banquesOptions = [...this.banquesOptions, banque];
          });
        }

        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement de l\'intérêt.' }); // Updated message
        this.loading = false;
        this.router.navigate(['/portail/interets']);
      },
    });
  }

  get f() {
    return this.interetForm.controls;
  }

  onSubmit(): void {
    this.interetForm.markAllAsTouched();

    if (this.interetForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cet intérêt ?' : 'Êtes-vous sûr de vouloir créer cet intérêt ?', // Updated message
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.executeSave(),
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.interetForm.value;

    // Prepare request DTO
    const request: InteretRequestDto = {
      banqueId: formValue.banqueId,
      libelle: formValue.libelle,
      observation: formValue.observation,
      montant: formValue.montant,
      // Ensure date is in a format accepted by the backend (e.g., ISO string)
      dateEmission: formValue.dateEmission.toISOString(),
    };

    if (this.isEditMode && this.interetId) {
      this.interetService.updateInterest(this.interetId, request).subscribe({ // Updated service call
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Intérêt mis à jour.', life: 3000 }); // Updated message
          this.loading = false;
          this.router.navigate(['/portail/interets']);
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la mise à jour.', life: 3000 });
          this.loading = false;
        },
      });
    } else {
      this.interetService.createInterest(request).subscribe({ // Updated service call
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Intérêt créé.', life: 3000 }); // Updated message
          this.loading = false;
          this.router.navigate(['/portail/interets']);
        },
        error: (error) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.error?.message || 'Échec de la création.', life: 3000 });
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/interets']);
  }
}
