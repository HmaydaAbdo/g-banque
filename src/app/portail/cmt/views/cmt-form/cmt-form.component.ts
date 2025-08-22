import { Component, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {ActivatedRoute, Router, ParamMap, RouterLinkActive} from '@angular/router';

// PrimeNG Modules
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

// Services and DTOs
import { CmtService } from '../../services/CmtService';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import {CreditMoyenTermeResponseDto} from "../../dtos/CreditMoyenTermeResponseDto";
import {CreditMoyenTermeRequestDto} from "../../dtos/CreditMoyenTermeRequestDto";
import {BreadcrumbModule} from "primeng/breadcrumb";


@Component({
  selector: 'app-cmt-form',
  templateUrl: './cmt-form.component.html',
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
    RouterLinkActive
  ],
  providers: [MessageService, ConfirmationService]
})
export class CmtFormComponent implements OnInit {

  cmtForm!: FormGroup;
  isEditMode: boolean = false;
  creditId: number | null = null;
  banquesOptions: { label: string; value: number }[] = [];
  loading: boolean = false;
  items: MenuItem[] | undefined;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private cmtService: CmtService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/cmt', label:"Les Crédits Moyens Termes" },
    ]
    this.initForm();
    this.loadBanquesForDropdown();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.creditId = Number(id);
        this.loadCredit(this.creditId);
      } else {
        this.isEditMode = false;
        this.creditId = null;
      }
    });
  }

  initForm(): void {
    this.cmtForm = this.fb.group({
      designation: ['', Validators.required],
      banqueId: [null, Validators.required],
      nominal: [null, [Validators.required, Validators.min(0.01)]],
      periodicite: [null, [Validators.required, Validators.min(1)]],
      duree: [null, [Validators.required, Validators.min(1)]],
      echeance: [null, [Validators.required, Validators.min(0.01)]],
      taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      dateDebut: [null, Validators.required],
    });
  }

  loadBanquesForDropdown(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc')
      .subscribe({
        next: (pageResponse: PageResponse<BanqueResponse>) => {
          this.banquesOptions = pageResponse.content.map(banque => ({
            label: banque.nom,
            value: banque.id!
          }));
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques.', life: 3000 });
          console.error('Error loading banks:', err);
        }
      });
  }

  loadCredit(id: number): void {
    this.loading = true;
    this.cmtService.getCreditById(id).subscribe({
      next: (credit: CreditMoyenTermeResponseDto) => {
        this.cmtForm.patchValue({
          designation: credit.designation,
          banqueId: credit.banque.id,
          nominal: credit.nominal,
          periodicite: credit.periodicite,
          duree: credit.duree,
          echeance: credit.echeance,
          taux: credit.taux,
          dateDebut: credit.dateDebut ? new Date(credit.dateDebut) : null,
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du crédit:', error);
        const errorMessage = error.error?.message || 'Impossible de charger les détails du crédit.';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
        this.loading = false;
        this.router.navigate(['/portail/cmt/list']);
      }
    });
  }

  get f() {
    return this.cmtForm.controls;
  }

  onSubmit(): void {
    this.cmtForm.markAllAsTouched();
    if (this.cmtForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Validation', detail: 'Veuillez corriger les erreurs dans le formulaire.', life: 3000 });
      return;
    }

    this.confirmationService.confirm({
      message: this.isEditMode ? 'Êtes-vous sûr de vouloir modifier ce crédit ?' : 'Êtes-vous sûr de vouloir créer ce crédit ?',
      header: 'Confirmation de l\'enregistrement',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSaveCredit();
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération d\'enregistrement annulée.', life: 3000 });
      }
    });
  }

  executeSaveCredit(): void {
    this.loading = true;
    const cmtRequest: CreditMoyenTermeRequestDto = this.cmtForm.value;

    if (this.isEditMode && this.creditId) {
      this.cmtService.updateCredit(this.creditId, cmtRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Crédit mis à jour avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/cmt/encours']);
          },
          error: (error) => {
            console.error('Erreur lors de la mise à jour du crédit:', error);
            const errorMessage = error.error?.message || 'Échec de la mise à jour du crédit.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    } else {
      this.cmtService.createCredit(cmtRequest)
        .subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Crédit créé avec succès.', life: 3000 });
            this.loading = false;
            this.router.navigate(['/portail/cmt/encours']);
          },
          error: (error) => {
            console.error('Erreur lors de la création du crédit:', error);
            const errorMessage = error.error?.message || 'Échec de la création du crédit.';
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage, life: 3000 });
            this.loading = false;
          }
        });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/cmt/list']);
  }
}
