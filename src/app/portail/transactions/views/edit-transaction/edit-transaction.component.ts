import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription } from 'rxjs';

// PrimeNG Modules
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
import { InputTextareaModule } from 'primeng/inputtextarea';

// Custom DTOs and Services
import { TransactionService } from "../../service/TransactionService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { TransactionResponseDto } from "../../dtos/TransactionResponseDto";
import { TransactionUpdateRequestDto } from "../../dtos/TransactionUpdateRequestDto";
import { BanqueResponse } from "../../../comptes-bancaire/dtos/BanqueResponse";
import { PageResponse } from "../../../global/types/PageResponse";
import { TransactionSource } from "../../dtos/TransactionSource";


@Component({
  selector: 'app-edit-transaction',
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
    RouterLink,
    RouterLinkActive,
    TooltipModule,
    InputTextareaModule,

  ],
  templateUrl: './edit-transaction.component.html',
  styleUrl: './edit-transaction.component.scss',
  providers: [MessageService, ConfirmationService],
})
export class EditTransactionComponent implements OnInit, OnDestroy {
  transactionForm!: FormGroup;
  transactionId: number | null = null;
  loading: boolean = false;

  banquesOptions: { label: string; value: number }[] = [];
  transactionSourceOptions: { label: string; value: TransactionSource }[] = [];

  items: MenuItem[] | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadDropdownData();
    this.setupBreadcrumbs();

    const paramMapSub = this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.transactionId = Number(id);
        this.loadTransaction(this.transactionId);
      } else {
        // Rediriger si l'ID est manquant (édition obligatoire)
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'ID de transaction manquant.', life: 3000 });
        this.router.navigate(['/portail/historique-des-transactions']);
      }
    });

    this.subscriptions.push(paramMapSub);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the form fields for TransactionUpdateRequestDto.
   */
  initForm(): void {
    this.transactionForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateValeur: [null, Validators.required],
      dateOperations: [null, Validators.required],
      libelle: ['', Validators.required],
      observation: ['',Validators.required],
      tvaDeductible: [null, Validators.required],
      tvaCollectee: [null, Validators.required],
      transactionSource: [null, Validators.required],
      debit: [null, Validators.required],
      credit: [null, Validators.required],
      banqueId: [null, Validators.required],
    });
  }

  /**
   * Loads the dropdown data (Banques and Transaction Sources).
   */
  loadDropdownData(): void {
    this.loading = true;

    // Load Banks
    const banquesSub = this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (pageResponse: PageResponse<BanqueResponse>) => {
        this.banquesOptions = pageResponse.content.map((banque) => ({
          label: banque.nom,
          value: banque.id!,
        }));
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des banques.', life: 3000 });
        this.loading = false;
        console.error('Error loading banks:', err);
      },
    });
    this.subscriptions.push(banquesSub);

    // Load Transaction Sources
    this.transactionSourceOptions = Object.values(TransactionSource).map(source => ({
      label: source, // Peut être ajusté pour une traduction si nécessaire
      value: source,
    }));
  }

  /**
   * Loads the existing transaction data and patches the form using the dedicated service method.
   */
  loadTransaction(id: number): void {
    this.loading = true;
    const loadSub = this.transactionService.getTransactionById(id).subscribe({
      next: (transaction: TransactionResponseDto) => {
        // Convert Decimal objects to string/number for form
        this.transactionForm.patchValue({
          tvaDeductible: transaction.tvaDeductible?.toString() || null,
          tvaCollectee: transaction.tvaCollectee?.toString() || null,
          debit: transaction.debit?.toString() || null,
          credit: transaction.credit?.toString() || null,

          // Convert Date strings to Date objects for p-calendar
          dateEmission: new Date(transaction.dateEmission),
          dateValeur: new Date(transaction.dateValeur),
          dateOperations: new Date(transaction.dateOperations),

          // Other fields
          libelle: transaction.libelle,
          observation: transaction.observation,
          transactionSource: transaction.transactionSource,
          banqueId: transaction.banqueDePrelevementId,
        });
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: `Échec du chargement de la transaction ID ${id}. L'ID n'existe peut-être pas.`,
          life: 3000
        });
        this.loading = false;
        this.router.navigate(['/portail/historique-des-transactions']);
        console.error('Error loading transaction:', err);
      },
    });
    this.subscriptions.push(loadSub);
  }


  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/historique-des-transactions', label: 'Historique des transactions' },
    ];
  }

  get f() {
    return this.transactionForm.controls;
  }

  onSubmit(): void {
    this.transactionForm.markAllAsTouched();
    this.transactionForm.updateValueAndValidity();

    if (this.transactionForm.invalid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs dans le formulaire.',
        life: 3000,
      });
      return;
    }

    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir modifier cette transaction ?',
      header: 'Confirmation de modification',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.executeSave();
      },
      reject: () => {
        this.messageService.add({
          severity: 'info',
          summary: 'Annulé',
          detail: 'Opération de modification annulée.',
          life: 3000,
        });
      },
    });
  }

  /**
   * Executes the update logic.
   */
  executeSave(): void {
    this.loading = true;
    const formValue = this.transactionForm.value;

    const request: TransactionUpdateRequestDto = {
      dateEmission: formValue.dateEmission,
      dateValeur: formValue.dateValeur,
      dateOperations: formValue.dateOperations,
      libelle: formValue.libelle,
      observation: formValue.observation,
      tvaDeductible: formValue.tvaDeductible,
      tvaCollectee: formValue.tvaCollectee,
      transactionSource: formValue.transactionSource,
      debit: formValue.debit,
      credit: formValue.credit,
      banqueId: formValue.banqueId,
    };


    if (this.transactionId) {
      this.transactionService.updateTransaction(this.transactionId, request).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Transaction mise à jour avec succès.',
            life: 3000,
          });
          this.loading = false;
          this.router.navigate(['/portail/historique-des-transactions']);
        },
        error: (error) => {
          const errorMessage = error.error?.message || 'Échec de la mise à jour de la transaction.';
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: errorMessage,
            life: 3000,
          });
          this.loading = false;
        },
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/portail/historique-des-transactions']);
  }
}
