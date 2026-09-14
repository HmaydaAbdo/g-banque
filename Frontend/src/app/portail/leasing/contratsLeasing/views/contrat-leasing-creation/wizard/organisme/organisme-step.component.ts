// src/app/contrat-leasing-creation/wizard/organisme/organisme-step.component.ts

import { Component, OnInit, OnDestroy, Output, EventEmitter, Input, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, Subject } from 'rxjs';
import { takeUntil, distinctUntilChanged } from 'rxjs/operators'; // Ensure distinctUntilChanged is imported

// PrimeNG Modules
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { Message, MessageService } from 'primeng/api';
import { MessagesModule } from "primeng/messages";

// Services and DTOs
import { OrganismeLeasingService } from "../../../../../organismesLeasing/services/organisme-leasing.service";
import { PageResponse } from "../../../../../../global/types/PageResponse";
import { OrganismeLeasingResponse } from "../../../../../organismesLeasing/dtos/OrganismeLeasingResponse";
import { LeasingContractWizardService } from '../../../../services/LeasingContractWizardService';


@Component({
  selector: 'app-organisme-step',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DropdownModule,
    InputTextModule,
    MessageModule,
    MessagesModule
  ],
  templateUrl: './organisme-step.component.html',
  // styleUrls: ['./organisme-step.component.scss'], // Uncomment if you have a stylesheet
  providers: [MessageService], // Provide MessageService if used for local step messages
})
export class OrganismeStepComponent implements OnInit, OnDestroy {

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
  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly messageService = inject(MessageService);
  private readonly wizardService = inject(LeasingContractWizardService);

  formGroup!: FormGroup;

  organismes: { label: string, value: number }[] = [];
  private subscriptions: Subscription[] = [];

  private componentDestroyed$ = new Subject<void>();

  ngOnInit(): void {

    this.formGroup = this.fb.group({
      organismeLeasingId: [null, Validators.required]
    });


    this.loadOrganismeOptions();

    // 1. Subscribe to wizardService.contract$ for initial state and external updates
    const contractSub = this.wizardService.contract$.pipe(
      distinctUntilChanged((prev, curr) => prev.organismeLeasingId === curr.organismeLeasingId),
      takeUntil(this.componentDestroyed$)
    ).subscribe(contract => {


      // Only patch if the value from the service is different from the current form value
      if (this.organismeLeasingIdControl?.value !== contract.organismeLeasingId) {
        this.formGroup.patchValue({
          organismeLeasingId: contract.organismeLeasingId
        }, { emitEvent: false });
      }

      // Force re-evaluation of validity after patching
      this.formGroup.updateValueAndValidity();
    });
    this.subscriptions.push(contractSub);

    // 2. Subscribe to local form changes to update central state
    const valueChangeSub = this.formGroup.valueChanges.pipe(
      // FIX: Add distinctUntilChanged here to prevent updating service if form value hasn't truly changed
      distinctUntilChanged((prev, curr) => prev.organismeLeasingId === curr.organismeLeasingId),
      takeUntil(this.componentDestroyed$)
    ).subscribe(value => {
      // Push only the relevant part of the DTO to the central service
      this.wizardService.updateContract({
        organismeLeasingId: value.organismeLeasingId
      });
    });
    this.subscriptions.push(valueChangeSub);

    // 3. Subscribe to local form status changes to emit local validity to parent
    const statusChangeSub = this.formGroup.statusChanges.pipe(
      distinctUntilChanged() // Only emit if status actually changes (e.g., from INVALID to VALID)
    ).subscribe(status => {
      this.stepValid.emit(status === 'VALID');
    });
    this.subscriptions.push(statusChangeSub);

    // Emit initial validity state
    this.stepValid.emit(this.formGroup.valid);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();

  }

  loadOrganismeOptions(): void {
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 100)
      .subscribe({
        next: (response: PageResponse<OrganismeLeasingResponse>) => {
          this.organismes = response.content.map(org => ({
            label: org.nom,
            value: org.id!
          }));


          const currentContract = this.wizardService.getCurrentContract();
          // Use the getter for the control and check its value
          if (currentContract.organismeLeasingId !== null && this.organismeLeasingIdControl?.value !== currentContract.organismeLeasingId) {
            this.formGroup.patchValue({
              organismeLeasingId: currentContract.organismeLeasingId
            }, { emitEvent: false });
            this.formGroup.updateValueAndValidity();
          }
        },
        error: () => {}

      });
  }

  markFormAsTouched(): void {
    this.formGroup.markAllAsTouched();
    this.formGroup.updateValueAndValidity();

  }

  setGlobalValidationMessages(messages: string[]): void {
    this.globalValidationMessages = messages;

  }

  get organismeLeasingIdControl(): FormControl | null {
    return this.formGroup.get('organismeLeasingId') as FormControl;
  }
}
