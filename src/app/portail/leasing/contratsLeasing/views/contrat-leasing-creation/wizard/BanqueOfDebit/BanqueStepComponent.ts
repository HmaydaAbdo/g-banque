// src/app/contrat-leasing-creation/wizard/BanqueOfDebit/BanqueStepComponent.ts

import { Component, Input, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
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

  formGroup!: FormGroup;

  banques: { label: string, value: number }[] = [];
  private subscriptions: Subscription[] = [];

  private componentDestroyed$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private banqueService: BanqueService,
    private messageService: MessageService,
    private wizardService: LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    console.log('BanqueStepComponent ngOnInit called');

    // 1. Initialize local FormGroup with validators
    this.formGroup = this.fb.group({
      banqueDePrelevementId: [null, Validators.required]
    });

    console.log('BanqueStepComponent: Form initialized. Valid:', this.formGroup.valid, 'Value:', this.formGroup.value);

    // 2. Load dropdown options
    this.loadBanqueOptions();

    // 3. Subscribe to central state to initialize local form with existing data
    const contractSub = this.wizardService.contract$.pipe(
      // Use distinctUntilChanged to only react when the relevant property for this step changes.
      // This is more robust than a custom filter and prevents unnecessary re-patches.
      distinctUntilChanged((prev, curr) => prev.banqueDePrelevementId === curr.banqueDePrelevementId),
      takeUntil(this.componentDestroyed$) // Ensure subscription is cleaned up
    ).subscribe(contract => {
      console.log('BanqueStepComponent: Received contract update from service:', contract.banqueDePrelevementId);

      // Only patch if the value from the service is different from the current form value
      if (this.banqueDePrelevementIdControl?.value !== contract.banqueDePrelevementId) {
        this.formGroup.patchValue({
          banqueDePrelevementId: contract.banqueDePrelevementId
        }, { emitEvent: false }); // Do NOT emit event here to prevent loop
        console.log('BanqueStepComponent: Form patched. Valid:', this.formGroup.valid, 'Value:', this.formGroup.value);
      } else {
        console.log('BanqueStepComponent: Service update received, but form value already matches. No patch needed.');
      }

      // NEW: Force re-evaluation of validity after patching
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
      console.log('BanqueStepComponent: Form value changed, updated service with ID:', value.banqueDePrelevementId);
    });
    this.subscriptions.push(valueChangeSub);

    // 5. Subscribe to local form status changes to emit local validity to parent
    const statusChangeSub = this.formGroup.statusChanges.pipe(
      distinctUntilChanged() // Only emit if status actually changes (e.g., from INVALID to VALID)
      // takeUntil(this.componentDestroyed$) // Already there in the original code. Good.
    ).subscribe(status => {
      this.stepValid.emit(status === 'VALID');
      console.log('BanqueStepComponent: Form status changed to:', status, 'Overall form valid:', this.formGroup.valid);
    });
    this.subscriptions.push(statusChangeSub);

    // Emit initial validity state immediately on init
    this.stepValid.emit(this.formGroup.valid);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
    console.log('BanqueStepComponent ngOnDestroy called');
  }

  loadBanqueOptions(): void {
    this.banqueService.getAllBanques({}, 0, 100) // Assuming a search method exists
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banques = response.content.map(bank => ({
            label: bank.nom, // Assuming 'nom' is the bank name field
            value: bank.id!
          }));
          console.log('BanqueStepComponent: Banque options loaded:', this.banques);

          // IMPORTANT: After options are loaded, re-patch the form in case the contract
          // was loaded from the service *before* options were available (race condition).
          // Use getCurrentContract() for synchronous access.
          const currentContract = this.wizardService.getCurrentContract();
          if (currentContract.banqueDePrelevementId !== null && this.banqueDePrelevementIdControl?.value !== currentContract.banqueDePrelevementId) {
            this.formGroup.patchValue({
              banqueDePrelevementId: currentContract.banqueDePrelevementId
            }, { emitEvent: false });
            // NEW: Force re-evaluation of validity here too after patching if options just loaded
            this.formGroup.updateValueAndValidity();
            console.log('BanqueStepComponent: Re-patched form after options loaded with ID:', currentContract.banqueDePrelevementId, 'Valid:', this.formGroup.valid);
          } else {
            console.log('BanqueStepComponent: Banque options loaded, but form value already matches service. No re-patch needed.');
          }
        },
        error: (error) => {
          console.error('Error loading bank options:', error);
        }
      });
  }

  // Public method for parent to call to mark form as touched
  markFormAsTouched(): void {
    this.formGroup.markAllAsTouched();
    this.formGroup.updateValueAndValidity(); // Ensure validation state is updated
    console.log('BanqueStepComponent: markFormAsTouched called. Form Valid:', this.formGroup.valid);
  }

  // Public method for parent to call to set global validation messages
  setGlobalValidationMessages(messages: string[]): void {
    this.globalValidationMessages = messages;
    console.log('BanqueStepComponent: setGlobalValidationMessages called with:', messages);
  }

  get banqueDePrelevementIdControl(): FormControl | null {
    return this.formGroup.get('banqueDePrelevementId') as FormControl;
  }
}
