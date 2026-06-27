import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {CalendarModule} from 'primeng/calendar';
import {FileUploadModule} from 'primeng/fileupload';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ImageModule} from 'primeng/image';
import {ToastModule} from 'primeng/toast';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {BanqueRequest} from '../../dtos/BanqueRequest';
import {BanqueResponse} from '../../dtos/BanqueResponse';
import {Devise} from '../../enums/Devise';
import {BanqueService} from '../../services/BanqueService';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {AuthService} from "../../../../auth/services/auth.service";
import {BreadcrumbModule} from "primeng/breadcrumb";

@Component({
  selector: 'app-banque-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    CalendarModule,
    FileUploadModule,
    ButtonModule,
    RippleModule,
    ImageModule,
    ToastModule,
    RouterLink,
    ConfirmDialogModule,
    BreadcrumbModule
  ],
  templateUrl: './banque-edit.component.html',
  styleUrl: './banque-edit.component.scss',
  providers: [
    MessageService,
    ConfirmationService
  ]
})
export class BanqueEditComponent implements OnInit {

  banqueForm!: FormGroup;
  originalBanqueResponse: BanqueResponse | null = null;
  logoFile: File | null = null;
  devises: string[] = Object.values(Devise);
  loading: boolean = false;
  isEditMode: boolean = false;
  items: MenuItem[] | undefined;

  private fb = inject(FormBuilder);
  private banqueService = inject(BanqueService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.initializeForm(); // Create the structure first

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label: "Comptes bancaires" }
    ];

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        // Disable the field immediately when we detect edit mode
        this.banqueForm.get('numeroDeCompte')?.disable();
        this.loadBanque(Number(id));
      } else {
        this.isEditMode = false;
        this.banqueForm.get('numeroDeCompte')?.enable();
      }
    });
  }

  initializeForm(): void {
    this.banqueForm = this.fb.group({
      nom: ['', Validators.required],
      // Pattern and length validators remain for creation mode
      numeroDeCompte: ['', [Validators.required, Validators.pattern(/^\d+$/), Validators.maxLength(24), Validators.minLength(24)]],
      devise: [Devise.MAD, Validators.required],
      adresse: ['', Validators.required],
      agence: ['', Validators.required],
    });
  }

  loadBanque(id: number): void {
    this.loading = true;
    this.banqueService.getBanqueById(id).subscribe({
      next: (data: BanqueResponse) => {
        this.originalBanqueResponse = data;
        this.banqueForm.patchValue({
          nom: data.nom,
          numeroDeCompte: data.numeroDeCompte,
          devise: data.devise,
          adresse: data.adresse,
          agence: data.agence,
        });
        // Ensure disabling happens after patching value if it's edit mode
        if (this.isEditMode && !this.authService.isSystemAdmin()) {
          this.banqueForm.get('numeroDeCompte')?.disable();
        }

        this.loading = false;
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Banque chargée avec succès.', life: 1000 });
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/portail/comptes-bancaires']);
      }
    });
  }

  onLogoSelect(event: any): void {
    if (event.files && event.files.length > 0) {
      this.logoFile = event.files[0];
      this.messageService.add({ severity: 'info', summary: 'Fichier sélectionné', detail: event.files[0].name });
    }
  }

  // Helper pour un accès facile aux contrôles du formulaire dans le template
  get f(): { [key: string]: AbstractControl } {
    return this.banqueForm.controls;
  }

  saveBanque(): void {

    this.banqueForm.markAllAsTouched();
    if (this.banqueForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    // Confirmation before saving
    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier cette banque ?' : 'Êtes-vous sûr de vouloir créer cette banque ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveBanque();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveBanque(): void {

    this.loading = true;
    const formValue = this.banqueForm.getRawValue(); // Use getRawValue to get disabled field values
    const banqueRequest: BanqueRequest = {
      nom: formValue.nom,
      numeroDeCompte: formValue.numeroDeCompte, // Will include value even if disabled
      devise: formValue.devise,
      adresse: formValue.adresse,
      agence: formValue.agence,
    };

    if (this.isEditMode && this.originalBanqueResponse?.id) {
      this.banqueService.updateBanque(this.originalBanqueResponse.id, banqueRequest, this.logoFile)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Banque mise à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/comptes-bancaires']); // Navigate after success
          },
          error: () => {
            this.loading = false;
          }
        });
    } else { // Create mode
      if (!this.logoFile) {
        this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez sélectionner un logo pour la nouvelle banque.', life: 3000 });
        this.loading = false; // Stop loading if logo is missing
        return;
      }
      this.banqueService.createBanque(banqueRequest, this.logoFile)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Banque créée avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/comptes-bancaires']); // Navigate after success
          },
          error: () => {
            this.loading = false;
          }
        });
    }
  }


}
