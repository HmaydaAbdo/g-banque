import { Component, Input, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, Subject } from 'rxjs';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';

// PrimeNG Modules
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber'; // Still needed for type, but not for valeurResiduelle input directly
import { MessageModule } from 'primeng/message';
import { Message, MessageService } from 'primeng/api';
import { TypeContratLeasing } from '../../../../dtos/type-contrat-leasing';
import { LeasingContractWizardService } from "../../../../services/LeasingContractWizardService";
import { MessagesModule } from "primeng/messages";
// import { Decimal } from 'decimal.js'; // No longer needed for this component

@Component({
  selector: 'app-contract-type-step',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DropdownModule,
    InputNumberModule, // Keep this import if it's used elsewhere for types, otherwise remove
    MessageModule,
    MessagesModule
  ],
  templateUrl: './contract-type-step.component.html',
  providers: [MessageService],
})
export class ContractTypeStepComponent implements OnInit, OnDestroy {

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
  contractTypes: { label: string, value: TypeContratLeasing }[] = [];
  private subscriptions: Subscription[] = [];
  public TypeContratLeasing = TypeContratLeasing;
  private componentDestroyed$ = new Subject<void>();


  constructor(
    private fb: FormBuilder,
    private wizardService: LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    console.log('ContractTypeStepComponent ngOnInit called');

    this.formGroup = this.fb.group({
      typeContrat: [null, Validators.required]
    });

    console.log('ContractTypeStepComponent: Form initialized. Valid:', this.formGroup.valid, 'Value:', this.formGroup.value);

    this.initContractTypes();

    const contractSub = this.wizardService.contract$.pipe(
      distinctUntilChanged((prev, curr) => {
        // Only compare typeContrat now
        return prev.typeContrat === curr.typeContrat;
      }),
      takeUntil(this.componentDestroyed$)
    ).subscribe(contract => {
      console.log('ContractTypeStepComponent: Received contract update from service:', contract);


      const currentFormValue = this.formGroup.value;
      const patchObject: { typeContrat?: TypeContratLeasing | null } = {};

      if (currentFormValue.typeContrat !== contract.typeContrat) {
        patchObject.typeContrat = contract.typeContrat;
      }

      if (Object.keys(patchObject).length > 0) {
        this.formGroup.patchValue(patchObject, { emitEvent: false });
        console.log('ContractTypeStepComponent: Form patched from service. Valid:', this.formGroup.valid, 'Value:', this.formGroup.value);
      } else {
        console.log('ContractTypeStepComponent: Service update received, but form values already match. No patch needed.');
      }


      this.formGroup.updateValueAndValidity();
    });
    this.subscriptions.push(contractSub);

    // 2. Subscribe to local form changes to update central state
    const valueChangeSub = this.formGroup.valueChanges.pipe(
      distinctUntilChanged((prev, curr) => {
        // Only compare typeContrat now
        return prev.typeContrat === curr.typeContrat;
      }),
      takeUntil(this.componentDestroyed$)
    ).subscribe(value => {
      console.log('ContractTypeStepComponent: Form value changed locally:', value);



      this.wizardService.updateContract({
        typeContrat: value.typeContrat
      });

    });
    this.subscriptions.push(valueChangeSub);

    // 3. Subscribe to local form status changes to emit local validity to parent
    const statusChangeSub = this.formGroup.statusChanges.pipe(
      distinctUntilChanged()
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
    console.log('ContractTypeStepComponent ngOnDestroy called');
  }

  initContractTypes(): void {
    this.contractTypes = [
      { label: 'Linéaire', value: TypeContratLeasing.LINEAIRE },
      { label: 'Progressif', value: TypeContratLeasing.PROGRESSIF },
      { label: 'Dégressif', value: TypeContratLeasing.DEGRESSIF }
    ];
  }

  get typeContratControl(): FormControl {
    return this.formGroup.get('typeContrat') as FormControl;
  }



}
