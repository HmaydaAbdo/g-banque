// src/app/contrat-leasing-creation/wizard/Phases/PhasesStepComponent.ts

import { Component, Input, OnInit, OnDestroy, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Subscription, Subject, BehaviorSubject, combineLatest } from 'rxjs';
import { takeUntil, distinctUntilChanged } from 'rxjs/operators';
import { Decimal } from 'decimal.js';

// Modules PrimeNG
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { MessageService, ConfirmationService } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { LeasingContractWizardService } from "../../../../services/LeasingContractWizardService";
import { ContratLeasingPhaseRequestDTO } from "../../../../dtos/contrat-leasing-phase";
import { TypeContratLeasing } from "../../../../dtos/type-contrat-leasing";
import { MessagesModule } from "primeng/messages";


@Component({
  selector: 'app-phases-step',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputNumberModule,
    ButtonModule,
    MessageModule,
    TableModule,
    DialogModule,
    ConfirmDialogModule,
    DecimalPipe,
    MessagesModule
  ],
  templateUrl: './phases-step.component.html',
  providers: [MessageService, ConfirmationService],
})
export class PhasesStepComponent implements OnInit, OnDestroy {

  @Output() stepValid = new EventEmitter<boolean>();

  @Input() globalValidationMessages: string[] = [];


  formGroup!: FormGroup;
  phasesForTable$: BehaviorSubject<FormGroup[]> = new BehaviorSubject<FormGroup[]>([]);

  displayPhaseDialog: boolean = false;
  phaseDialogHeader: string = '';
  phaseForm!: FormGroup;
  currentPhaseIndex: number = -1;

  currentTypeContrat: TypeContratLeasing | null = null;
  currentPeriodeEnMois: number | null = null;

  private subscriptions: Subscription[] = [];
  private componentDestroyed$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private wizardService: LeasingContractWizardService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    console.log('PhasesStepComponent: ngOnInit called.');
    this.initFormGroup()

    this.formGroup.updateValueAndValidity();


    const combinedServiceDataSub = combineLatest([
      this.wizardService.contract$,
      this.wizardService.globalValidationErrors$
    ]).pipe(
      takeUntil(this.componentDestroyed$),
      distinctUntilChanged(([prevContract, prevErrors], [currContract, currErrors]) => {
        const prevPhasesNormalized = prevContract.phases.map(p => ({
          ...p,
          mensualiteTTC: p.mensualiteTTC ? p.mensualiteTTC.toString() : null
        }));
        const currPhasesNormalized = currContract.phases.map(p => ({
          ...p,
          mensualiteTTC: p.mensualiteTTC ? p.mensualiteTTC.toString() : null
        }));

        const phasesChanged = JSON.stringify(prevPhasesNormalized) !== JSON.stringify(currPhasesNormalized);
        const typeContratChanged = prevContract.typeContrat !== currContract.typeContrat;
        const periodeEnMoisChanged = prevContract.periodeEnMois !== currContract.periodeEnMois;
        const errorsChanged = JSON.stringify(prevErrors) !== JSON.stringify(currErrors);

        if (!phasesChanged && !typeContratChanged && !periodeEnMoisChanged && !errorsChanged) {
          console.log('PhasesStepComponent: combinedServiceData$ distinctUntilChanged prevented emission (no relevant changes detected).');
        }

        return !phasesChanged && !typeContratChanged && !periodeEnMoisChanged && !errorsChanged;
      })
    ).subscribe(([contract, errors]) => {
      console.log('PhasesStepComponent: combinedServiceData$ subscription received update from service.');
      console.log('PhasesStepComponent: Current phases in service contract (received):', contract.phases.length, contract.phases);
      console.log('PhasesStepComponent: Global validation errors (received):', errors);

      this.globalValidationMessages = errors; // Update the @Input() property

      const previousTypeContrat = this.currentTypeContrat;
      this.currentTypeContrat = contract.typeContrat || null;

      this.currentPeriodeEnMois = contract.periodeEnMois || null;

      if (previousTypeContrat !== null && previousTypeContrat !== this.currentTypeContrat) {
        console.log(`PhasesStepComponent: Contract type changed from ${previousTypeContrat} to ${this.currentTypeContrat}.`);

        if (contract.phases.length > 0) {
          this.wizardService.clearPhases();
          this.messageService.add({
            severity: 'info',
            summary: 'Ajustement des phases',
            detail: 'Le type de contrat a changé. Les phases existantes ont été supprimées.'
          });
          return;
        }
      }

      this.updatePhasesFormArray(contract.phases || []);

      this.applyMandatoryPhaseValidation();

      this.formGroup.updateValueAndValidity();

      console.log('PhasesStepComponent: Form patched after service update. Valid:', this.formGroup.valid, 'Value:', this.formGroup.value);
    });
    this.subscriptions.push(combinedServiceDataSub);


    const phasesValueChangeSub = this.phases.valueChanges.pipe(
      takeUntil(this.componentDestroyed$),
      distinctUntilChanged((prevPhases, currPhases) => {
        const prevPhasesNormalized = prevPhases.map((p: any) => ({
          ...p,
          mensualiteTTC: p.mensualiteTTC ? p.mensualiteTTC.toString() : null
        }));
        const currPhasesNormalized = currPhases.map((p: any) => ({
          ...p,
          mensualiteTTC: p.mensualiteTTC ? p.mensualiteTTC.toString() : null
        }));

        if (JSON.stringify(prevPhasesNormalized) === JSON.stringify(currPhasesNormalized)) {
          console.log('PhasesStepComponent: phases.valueChanges distinctUntilChanged prevented emission (no deep changes).');
        }

        return JSON.stringify(prevPhasesNormalized) === JSON.stringify(currPhasesNormalized);
      })
    ).subscribe(phasesValue => {
      console.log('PhasesStepComponent: phases.valueChanges subscription triggered.');
      const phasesForService = phasesValue.map((phase: any) => ({
        ...phase,
        mensualiteTTC: new Decimal(phase.mensualiteTTC)
      }));
      this.wizardService.updateContract({ phases: phasesForService });
      console.log('PhasesStepComponent: Phases FormArray value changed, updated service with:', phasesForService);
    });
    this.subscriptions.push(phasesValueChangeSub);


    const phasesStatusSub = this.phases.statusChanges.pipe(
      takeUntil(this.componentDestroyed$),
      distinctUntilChanged()
    ).subscribe(status => {
      console.log('PhasesStepComponent: phases.statusChanges subscription triggered. Status:', status);
      Promise.resolve().then(() => {
        this.stepValid.emit(status === 'VALID');
        this.cdr.detectChanges();
        console.log('PhasesStepComponent: Emitted stepValid:', status === 'VALID', 'Overall form valid:', this.formGroup.valid);
      });
    });
    this.subscriptions.push(phasesStatusSub);

    Promise.resolve().then(() => {
      this.stepValid.emit(this.formGroup.valid);
      this.cdr.detectChanges();
      console.log('PhasesStepComponent: Initial stepValid emitted:', this.formGroup.valid);
    });
  }

  initFormGroup() {
    this.formGroup = this.fb.group({
      phases: this.fb.array([])
    });

    this.phaseForm = this.fb.group({
      numeroPhase: [null, Validators.required],
      dureeMois: [null, [Validators.required, Validators.min(1)]],
      mensualiteTTC: [null, [Validators.required, Validators.min(0.01)]]
    });

    console.log('PhasesStepComponent: Form initialized. Root form valid:', this.formGroup.valid, 'Value:', this.formGroup.value);

  }
  ngOnDestroy(): void {
    console.log('PhasesStepComponent: ngOnDestroy called. Unsubscribing all subscriptions.');
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
    console.log('PhasesStepComponent: All subscriptions unsubscribed.');
  }

  get phases(): FormArray {
    return this.formGroup.get('phases') as FormArray;
  }

  private createPhaseFormGroup(phase: ContratLeasingPhaseRequestDTO): FormGroup {
    console.log('PhasesStepComponent: createPhaseFormGroup called for phase:', phase.numeroPhase);
    return this.fb.group({
      numeroPhase: [phase.numeroPhase, Validators.required],
      dureeMois: [phase.dureeMois ?? 1, [Validators.required, Validators.min(1)]],
      mensualiteTTC: [phase.mensualiteTTC ? phase.mensualiteTTC.toNumber() : 0.01, [Validators.required, Validators.min(0.01)]]
    });
  }

  private updatePhasesFormArray(phasesDto: ContratLeasingPhaseRequestDTO[]): void {
    console.log('PhasesStepComponent: updatePhasesFormArray called with DTO:', phasesDto);
    console.log('PhasesStepComponent: Current FormArray controls BEFORE update:', this.phases.controls.length);

    const currentPhaseControls = this.phases.controls;
    const currentLength = currentPhaseControls.length;
    const dtoLength = phasesDto.length;

    for (let i = 0; i < Math.min(currentLength, dtoLength); i++) {
      const formGroup = currentPhaseControls[i] as FormGroup;
      const phaseDto = phasesDto[i];

      const patchValue = {
        numeroPhase: phaseDto.numeroPhase,
        dureeMois: phaseDto.dureeMois,
        mensualiteTTC: phaseDto.mensualiteTTC ? phaseDto.mensualiteTTC.toNumber() : null
      };

      if (formGroup.get('numeroPhase')?.value !== patchValue.numeroPhase ||
        formGroup.get('dureeMois')?.value !== patchValue.dureeMois ||
        formGroup.get('mensualiteTTC')?.value !== patchValue.mensualiteTTC) {

        formGroup.patchValue(patchValue, { emitEvent: false });
        formGroup.updateValueAndValidity({ emitEvent: false });
        console.log(`PhasesStepComponent: Patched existing phase ${i}. Current form value:`, formGroup.value);
      } else {
        console.log(`PhasesStepComponent: Phase ${i} already matches DTO. No patch needed.`);
      }
    }

    for (let i = currentLength; i < dtoLength; i++) {
      const newPhaseGroup = this.createPhaseFormGroup(phasesDto[i]);
      this.phases.push(newPhaseGroup, { emitEvent: false });
      console.log(`PhasesStepComponent: Added new phase ${i}. New FormArray length: ${this.phases.length}`);
    }

    for (let i = currentLength - 1; i >= dtoLength; i--) {
      this.phases.removeAt(i, { emitEvent: false });
      console.log(`PhasesStepComponent: Removed phase ${i}. New FormArray length: ${this.phases.length}`);
    }

    this.phases.updateValueAndValidity();
    console.log('PhasesStepComponent: Emitting updated phasesForTable$ with controls:', this.phases.controls.length, this.phases.controls);
    this.phasesForTable$.next([...this.phases.controls] as FormGroup[]);
    this.cdr.detectChanges();
    console.log('PhasesStepComponent: updatePhasesFormArray completed. FormArray Valid:', this.phases.valid, 'Current phases controls AFTER update:', this.phases.controls.length);
  }


  isLinearContract(): boolean {
    return this.currentTypeContrat === TypeContratLeasing.LINEAIRE;
  }

  canAddPhase(): boolean {
    return !(this.isLinearContract() && this.phases.length > 0);
  }

  private applyMandatoryPhaseValidation(): void {
    console.log('PhasesStepComponent: applyMandatoryPhaseValidation called. Applying minLength(1) validator.');
    this.phases.setValidators(Validators.minLength(1));
    this.phases.updateValueAndValidity();
    this.formGroup.updateValueAndValidity();
    console.log('PhasesStepComponent: applyMandatoryPhaseValidation completed. FormArray Valid:', this.phases.valid);
  }

  openNewPhaseDialog(): void {
    console.log('PhasesStepComponent: openNewPhaseDialog called.');
    if (!this.canAddPhase()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Pour un contrat Linéaire, une seule phase est autorisée.'
      });
      console.log('PhasesStepComponent: Cannot add phase (linear contract rule).');
      return;
    }

    this.phaseForm.reset();
    this.currentPhaseIndex = -1;
    this.phaseDialogHeader = 'Ajouter une nouvelle Phase';

    this.phaseForm.get('numeroPhase')?.setValue(this.wizardService.getCurrentContract().phases.length + 1);

    this.phaseForm.updateValueAndValidity();

    this.displayPhaseDialog = true;
    console.log('PhasesStepComponent: Opened new phase dialog. displayPhaseDialog:', this.displayPhaseDialog);
  }

  editPhase(phaseIndex: number): void {
    console.log('PhasesStepComponent: editPhase called for index:', phaseIndex);
    this.currentPhaseIndex = phaseIndex;
    this.phaseDialogHeader = 'Modifier la Phase ' + (phaseIndex + 1);
    const phaseToEdit = this.wizardService.getCurrentContract().phases[phaseIndex];

    this.phaseForm.setValue({
      numeroPhase: phaseToEdit.numeroPhase,
      dureeMois: phaseToEdit.dureeMois ?? 1,
      mensualiteTTC: phaseToEdit.mensualiteTTC ? phaseToEdit.mensualiteTTC.toNumber() : 0.01
    });


    this.phaseForm.updateValueAndValidity();

    this.displayPhaseDialog = true;
    console.log('PhasesStepComponent: Opened edit phase dialog for index:', phaseIndex, 'displayPhaseDialog:', this.displayPhaseDialog);
  }

  savePhase(): void {
    console.log('PhasesStepComponent: savePhase called.');
    this.phaseForm.updateValueAndValidity();

    if (this.phaseForm.invalid) {
      this.phaseForm.markAllAsTouched();
      this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Veuillez corriger les erreurs dans le formulaire de la phase.' });
      console.log('PhasesStepComponent: Phase form is INVALID. Not saving.');
      return;
    }

    let dureeMoisValue: number = this.phaseForm.get('dureeMois')?.value;

    const phaseDataForService: ContratLeasingPhaseRequestDTO = {
      numeroPhase: this.phaseForm.value.numeroPhase,
      dureeMois: dureeMoisValue,
      mensualiteTTC: new Decimal(this.phaseForm.value.mensualiteTTC)
    };

    if (this.currentPhaseIndex === -1) {
      this.wizardService.addPhase(phaseDataForService);
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Phase ajoutée avec succès.' });
      console.log('PhasesStepComponent: Called wizardService.addPhase with:', phaseDataForService);
    } else {
      this.wizardService.updatePhase(this.currentPhaseIndex, phaseDataForService);
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Phase modifiée avec succès.' });
      console.log('PhasesStepComponent: Called wizardService.updatePhase for index', this.currentPhaseIndex, 'with:', phaseDataForService);
    }

    console.log('PhasesStepComponent: Scheduling dialog close with setTimeout(0) to allow async updates to propagate.');
    setTimeout(() => {
      console.log('PhasesStepComponent: setTimeout(0) callback executed. Triggering change detection before dialog close.');
      this.cdr.detectChanges();
      this.displayPhaseDialog = false;
      console.log('PhasesStepComponent: Dialog displayPhaseDialog set to false.');
    }, 0);
  }

  cancelPhaseDialog(): void {
    console.log('PhasesStepComponent: cancelPhaseDialog called.');
    this.displayPhaseDialog = false;
    console.log('PhasesStepComponent: Cancelled phase dialog. displayPhaseDialog:', this.displayPhaseDialog);
  }

  deletePhase(index: number): void {
    console.log('PhasesStepComponent: deletePhase called for index:', index);
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette phase ?',
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        console.log('PhasesStepComponent: User confirmed deletion. Calling wizardService.removePhase.');
        this.wizardService.removePhase(index);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Phase supprimée avec succès.' });
        console.log('PhasesStepComponent: Called wizardService.removePhase for index:', index);
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Suppression de la phase annulée.' });
        console.log('PhasesStepComponent: Phase deletion cancelled.');
      }
    });
  }

  showPhasesRequiredForTypeError(): boolean {
    return this.globalValidationMessages.some(msg => msg.includes('phases:Des phases sont requises pour le type de contrat'));
  }

  get phasesRequiredForTypeMessage(): string {
    const msg: string | undefined = this.globalValidationMessages.find(m => m.includes('phases:Des phases sont requises pour le type de contrat'));
    return msg ? (msg.split(':')[1]?.trim() || msg) : '';
  }

  // NEW GETTER: To check if total duration mismatch error exists
  showTotalDurationMismatchError(): boolean {
    return this.globalValidationMessages.some(msg => msg.includes('phases:La durée totale des phases') && msg.includes('ne correspond pas'));
  }

  // NEW GETTER: To retrieve the total duration mismatch message
  get totalDurationMismatchMessage(): string {
    const msg: string | undefined = this.globalValidationMessages.find(m => m.includes('phases:La durée totale des phases') && m.includes('ne correspond pas'));
    // Extract the part after "phases:"
    return msg ? (msg.split(':')[1]?.trim() || msg) : '';
  }

  // NEW GETTER: To check if an invalid order error exists for progressive contracts
  showProgressiveOrderError(): boolean {
    return this.globalValidationMessages.some(msg => msg.includes('phases:L\'ordre des mensualités est invalide pour un contrat Progressif'));
  }

  // NEW GETTER: To retrieve the invalid order message for progressive contracts
  getProgressiveOrderMessage(): string {
    const msg: string | undefined = this.globalValidationMessages.find(m => m.includes('phases:L\'ordre des mensualités est invalide pour un contrat Progressif'));
    return msg ? (msg.split(':')[1]?.trim() || msg) : '';
  }

  // NEW GETTER: To check if an invalid order error exists for degressive contracts
  showDegressiveOrderError(): boolean {
    return this.globalValidationMessages.some(msg => msg.includes('phases:L\'ordre des mensualités est invalide pour un contrat Dégressif'));
  }

  // NEW GETTER: To retrieve the invalid order message for degressive contracts
  getDegressiveOrderMessage(): string {
    const msg: string | undefined = this.globalValidationMessages.find(m => m.includes('phases:L\'ordre des mensualités est invalide pour un contrat Dégressif'));
    return msg ? (msg.split(':')[1]?.trim() || msg) : '';
  }


  get dialogDureeMoisControl(): FormControl | null {
    return this.phaseForm.get('dureeMois') as FormControl;
  }

  get dialogMensualiteTTCControl(): FormControl | null {
    return this.phaseForm.get('mensualiteTTC') as FormControl;
  }
}
