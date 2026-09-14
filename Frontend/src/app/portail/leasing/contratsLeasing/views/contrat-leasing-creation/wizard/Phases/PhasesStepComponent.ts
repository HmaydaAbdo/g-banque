// src/app/contrat-leasing-creation/wizard/Phases/PhasesStepComponent.ts

import { Component, Input, OnInit, OnDestroy, Output, EventEmitter, ChangeDetectorRef, inject } from '@angular/core';
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


  private readonly fb = inject(FormBuilder);
  private readonly wizardService = inject(LeasingContractWizardService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly cdr = inject(ChangeDetectorRef);

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

  ngOnInit(): void {
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

        return !phasesChanged && !typeContratChanged && !periodeEnMoisChanged && !errorsChanged;
      })
    ).subscribe(([contract, errors]) => {
      this.globalValidationMessages = errors;

      const previousTypeContrat = this.currentTypeContrat;
      this.currentTypeContrat = contract.typeContrat || null;

      this.currentPeriodeEnMois = contract.periodeEnMois || null;

      if (previousTypeContrat !== null && previousTypeContrat !== this.currentTypeContrat) {
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

        return JSON.stringify(prevPhasesNormalized) === JSON.stringify(currPhasesNormalized);
      })
    ).subscribe(phasesValue => {
      const phasesForService = phasesValue.map((phase: any) => ({
        ...phase,
        mensualiteTTC: new Decimal(phase.mensualiteTTC)
      }));
      this.wizardService.updateContract({ phases: phasesForService });
    });
    this.subscriptions.push(phasesValueChangeSub);


    const phasesStatusSub = this.phases.statusChanges.pipe(
      takeUntil(this.componentDestroyed$),
      distinctUntilChanged()
    ).subscribe(status => {
      Promise.resolve().then(() => {
        this.stepValid.emit(status === 'VALID');
        this.cdr.detectChanges();
      });
    });
    this.subscriptions.push(phasesStatusSub);

    Promise.resolve().then(() => {
      this.stepValid.emit(this.formGroup.valid);
      this.cdr.detectChanges();
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


  }
  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
  }

  get phases(): FormArray {
    return this.formGroup.get('phases') as FormArray;
  }

  private createPhaseFormGroup(phase: ContratLeasingPhaseRequestDTO): FormGroup {
    return this.fb.group({
      numeroPhase: [phase.numeroPhase, Validators.required],
      dureeMois: [phase.dureeMois ?? 1, [Validators.required, Validators.min(1)]],
      mensualiteTTC: [phase.mensualiteTTC ? phase.mensualiteTTC.toNumber() : 0.01, [Validators.required, Validators.min(0.01)]]
    });
  }

  private updatePhasesFormArray(phasesDto: ContratLeasingPhaseRequestDTO[]): void {

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
      } else {
      }
    }

    for (let i = currentLength; i < dtoLength; i++) {
      const newPhaseGroup = this.createPhaseFormGroup(phasesDto[i]);
      this.phases.push(newPhaseGroup, { emitEvent: false });
    }

    for (let i = currentLength - 1; i >= dtoLength; i--) {
      this.phases.removeAt(i, { emitEvent: false });
    }

    this.phases.updateValueAndValidity();
    this.phasesForTable$.next([...this.phases.controls] as FormGroup[]);
    this.cdr.detectChanges();
  }


  isLinearContract(): boolean {
    return this.currentTypeContrat === TypeContratLeasing.LINEAIRE;
  }

  canAddPhase(): boolean {
    return !(this.isLinearContract() && this.phases.length > 0);
  }

  private applyMandatoryPhaseValidation(): void {
    this.phases.setValidators(Validators.minLength(1));
    this.phases.updateValueAndValidity();
    this.formGroup.updateValueAndValidity();
  }

  openNewPhaseDialog(): void {
    if (!this.canAddPhase()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Pour un contrat Linéaire, une seule phase est autorisée.'
      });
      return;
    }

    this.phaseForm.reset();
    this.currentPhaseIndex = -1;
    this.phaseDialogHeader = 'Ajouter une nouvelle Phase';

    this.phaseForm.get('numeroPhase')?.setValue(this.wizardService.getCurrentContract().phases.length + 1);

    this.phaseForm.updateValueAndValidity();

    this.displayPhaseDialog = true;
  }

  editPhase(phaseIndex: number): void {
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
  }

  savePhase(): void {
    this.phaseForm.updateValueAndValidity();

    if (this.phaseForm.invalid) {
      this.phaseForm.markAllAsTouched();
      // Erreur affichée via les messages inline du formulaire — l'interceptor gère les erreurs HTTP
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
    } else {
      this.wizardService.updatePhase(this.currentPhaseIndex, phaseDataForService);
      this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Phase modifiée avec succès.' });
    }

    setTimeout(() => {
      this.cdr.detectChanges();
      this.displayPhaseDialog = false;
    }, 0);
  }

  cancelPhaseDialog(): void {
    this.displayPhaseDialog = false;
  }

  deletePhase(index: number): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette phase ?',
      header: 'Confirmation de Suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.wizardService.removePhase(index);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Phase supprimée avec succès.' });
      },
      reject: () => {
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Suppression de la phase annulée.' });
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
