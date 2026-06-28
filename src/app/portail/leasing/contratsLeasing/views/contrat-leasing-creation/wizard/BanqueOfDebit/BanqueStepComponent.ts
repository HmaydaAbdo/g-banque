// src/app/contrat-leasing-creation/wizard/BanqueOfDebit/BanqueStepComponent.ts

import { Component, Input, OnInit, OnDestroy, Output, EventEmitter, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, Subject } from 'rxjs';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators'; // Import distinctUntilChanged

// PrimeNG Modules
import { DropdownModule } from 'primeng/dropdown';
import { MessageModule } from 'primeng/message';
import { Message, MessageService } from 'primeng/api';
import { MessagesModule } from "primeng/messages";

// Services and DTOs
import { BanqueService } from "../../../../../../comptes-bancaire/services/BanqueService";
import { PageResponse } from "../../../../../../global/types/PageResponse";
import { BanqueResponse } from "../../../../../../comptes-bancaire/dtos/BanqueResponse";
import { LeasingContractWizardService } from '../../../../services/LeasingContractWizardService';


@Component({
  selector: 'app-banque-step',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DropdownModule,
    MessageModule,
    MessagesModule
  ],
  templateUrl: './banque-step.component.html',
  // styleUrls: ['./banque-step.component.scss'], // Uncomment if you have a stylesheet
  providers: [MessageService], // Provide MessageService if used for local step messages
})
export class BanqueStepComponent implements OnInit, OnDestroy {

  @Output() stepValid = new EventEmitter<boolean>();

  @Input() globalValidationMessages: string[] = [];

  get formattedGlobalMessages(): Message[] {
    return this.globalValidationMessages.map(msg => ({
      severity: 'error',
      summary: 'Erreur',
      detail: msg
    }));
  }

  private readonly fb = inject(FormBuilder);
  private readonly banqueService = inject(BanqueService);
  private readonly messageService = inject(MessageService);
  private readonly wizardService = inject(LeasingContractWizardService);

  formGroup!: FormGroup;

  banques: { label: string, value: number }[] = [];
  private subscriptions: Subscription[] = [];

  private componentDestroyed$ = new Subject<void>();

  ngOnInit(): void {
    // 1. Initialize local FormGroup with validators
    this.formGroup = this.fb.group({
      banqueDePrelevementId: [null, Validators.required]
    });

    // 2. Load dropdown options
    this.loadBanqueOptions();

    // 3. Subscribe to central state to initialize local form with existing data
    const contractSub = this.wizardService.contract$.pipe(
      // Use distinctUntilChanged to only react when the relevant property for this step changes.
      // This is more robust than a custom filter and prevents unnecessary re-patches.
      distinctUntilChanged((prev, curr) => prev.banqueDePrelevementId === curr.banqueDePrelevementId),
      takeUntil(this.componentDestroyed$) // Ensure subscription is cleaned up
    ).subscribe(contract => {
      // Only patch if the value from the service is different from the current form value
      if (this.banqueDePrelevementIdControl?.value !== contract.banqueDePrelevementId) {
        this.formGroup.patchValue({
          banqueDePrelevementId: contract.banqueDePrelevementId
        }, { emitEvent: false });
      }

      // Force re-evaluation of validity after patching
      this.formGroup.updateValueAndValidity();
    });
    this.subscriptions.push(contractSub);

    // 4. Subscribe to local form changes to update central state
    const valueChangeSub = this.formGroup.valueChanges.pipe(
      // FIX: Add distinctUntilChanged here to prevent updating service if form value hasn't truly changed
      distinctUntilChanged((prev, curr) => prev.banqueDePrelevementId === curr.banqueDePrelevementId),
      takeUntil(this.componentDestroyed$) // Add takeUntil for this subscription too
    ).subscribe(value => {
      // Push only the relevant part of the DTO to the central service
      this.wizardService.updateContract({
        banqueDePrelevementId: value.banqueDePrelevementId
      });
    });
    this.subscriptions.push(valueChangeSub);

    // 5. Subscribe to local form status changes to emit local validity to parent
    const statusChangeSub = this.formGroup.statusChanges.pipe(
      distinctUntilChanged() // Only emit if status actually changes (e.g., from INVALID to VALID)
      // takeUntil(this.componentDestroyed$) // Already there in the original code. Good.
    ).subscribe(status => {
      this.stepValid.emit(status === 'VALID');
    });
    this.subscriptions.push(statusChangeSub);

    // Emit initial validity state immediately on init
    this.stepValid.emit(this.formGroup.valid);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
  }

  loadBanqueOptions(): void {
    this.banqueService.getAllBanques({}, 0, 100) // Assuming a search method exists
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banques = response.content.map(bank => ({
            label: bank.nom,
            value: bank.id!
          }));

          // IMPORTANT: After options are loaded, re-patch the form in case the contract
          // was loaded from the service *before* options were available (race condition).
          // Use getCurrentContract() for synchronous access.
          const currentContract = this.wizardService.getCurrentContract();
          if (currentContract.banqueDePrelevementId !== null && this.banqueDePrelevementIdControl?.value !== currentContract.banqueDePrelevementId) {
            this.formGroup.patchValue({
              banqueDePrelevementId: currentContract.banqueDePrelevementId
            }, { emitEvent: false });
            this.formGroup.updateValueAndValidity();
          }
        },
        error: () => {}

      });
  }

  // Public method for parent to call to mark form as touched
  markFormAsTouched(): void {
    this.formGroup.markAllAsTouched();
    this.formGroup.updateValueAndValidity();
  }

  setGlobalValidationMessages(messages: string[]): void {
    this.globalValidationMessages = messages;
  }

  get banqueDePrelevementIdControl(): FormControl | null {
    return this.formGroup.get('banqueDePrelevementId') as FormControl;
  }
}
