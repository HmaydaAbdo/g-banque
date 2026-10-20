// src/app/contrat-leasing-creation/wizard/GeneralDetail/GeneralDetailsStepComponent.ts

import { Component, Input, OnInit, OnDestroy, Output, EventEmitter, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, Subject } from 'rxjs';
import { distinctUntilChanged, takeUntil, debounceTime } from 'rxjs/operators'; // Import debounceTime
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { Message } from 'primeng/api';
import { MessagesModule } from 'primeng/messages';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { LeasingContractWizardService } from "../../../../services/LeasingContractWizardService";
import { Decimal } from 'decimal.js';
import { toLocalDateString } from '../../../../../../global/types/date.utils';
import { EcheanceLeasingService } from '../../../../services/EcheanceLeasingService';
import { LIBELLE_TYPE_MOYEN_PAIEMENT, MoyenPaiementAvance, TypeMoyenPaiementAvance } from '../../../../dtos/avance-leasing';

@Component({
  selector: 'app-general-details-step',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    CalendarModule,
    InputNumberModule,
    MessageModule,
    MessagesModule,
    DropdownModule,
    ButtonModule,
    TagModule
  ],
  templateUrl: './general-details-step.component.html',
  styleUrls: ['./general-details-step.component.scss'],
})
export class GeneralDetailsStepComponent implements OnInit, OnDestroy {

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
  private readonly wizardService = inject(LeasingContractWizardService);
  private readonly echeanceLeasingService = inject(EcheanceLeasingService);

  // ── Avance : choix d'un chèque / d'une LCN / d'un virement existant (liste déroulante à chargement progressif) ──
  private static readonly TAILLE_PAGE = 20;
  readonly typesMoyen: { label: string; value: TypeMoyenPaiementAvance }[] = [
    { label: 'Chèque', value: 'CHEQUE' },
    { label: 'LCN', value: 'LCN' },
    { label: 'Virement', value: 'VIREMENT' }
  ];
  readonly libelleType = LIBELLE_TYPE_MOYEN_PAIEMENT;
  /** Moyen de paiement retenu pour l'avance (null : pas d'avance). */
  avanceChoisie: MoyenPaiementAvance | null = null;
  /** Options de la liste : une case par résultat du serveur, remplie à mesure que l'on fait défiler. */
  moyens: (MoyenPaiementAvance | undefined)[] = [];
  chargementMoyens = false;
  private pagesChargees = new Set<number>();
  /** Numéro saisi dans la zone de recherche de la liste. */
  private recherche = '';

  formGroup!: FormGroup;
  private subscriptions: Subscription[] = [];
  private componentDestroyed$ = new Subject<void>();

  ngOnInit(): void {

    this.formGroup = this.fb.group({
      numeroContrat: [null, [Validators.required]],
      immobilisation: [null, [Validators.required]],
      montantAchatTTC: [null, [Validators.required, Validators.min(0.01)]],
      dateDebut: [null, Validators.required],
      periodeEnMois: [null, [Validators.required, Validators.min(1), Validators.max(360)]],
      valeurResiduelle: [{ value: null, disabled: true }], // Ensure it's disabled and read-only
      typeMoyenAvance: [null as TypeMoyenPaiementAvance | null],
      moyenAvance: [{ value: null as MoyenPaiementAvance | null, disabled: true }]
    });

    this.typeMoyenAvanceControl.valueChanges.pipe(takeUntil(this.componentDestroyed$)).subscribe(type => {
      if (type) {
        this.moyenAvanceControl.enable({ emitEvent: false });
        this.rechargerMoyens();
      } else {
        this.moyenAvanceControl.disable({ emitEvent: false });
        this.moyens = [];
      }
    });
    // Le choix dans la liste est enregistré tout de suite ; la liste revient à « Choisir… » (le moyen retenu s'affiche en dessous)
    this.moyenAvanceControl.valueChanges.pipe(takeUntil(this.componentDestroyed$)).subscribe(moyen => {
      if (moyen) {
        this.wizardService.updateContract({ avanceMoyenPaiement: moyen });
        this.moyenAvanceControl.setValue(null, { emitEvent: false });
      }
    });


    // 1. Subscribe to central state to initialize local form
    const contractSub = this.wizardService.contract$.pipe(
      distinctUntilChanged((prev, curr) => {
        // Deep comparison for objects from service
        const prevMontantAchatTTC = prev.montantAchatTTC;
        const currMontantAchatTTC = curr.montantAchatTTC;

        const prevValeurResiduelle = prev.valeurResiduelle;
        const currValeurResiduelle = curr.valeurResiduelle;

        const prevDateDebutISO = prev.dateDebut ? toLocalDateString(prev.dateDebut) : null;
        const currDateDebutISO = curr.dateDebut ? toLocalDateString(curr.dateDebut) : null;

        return prev.numeroContrat === curr.numeroContrat &&
          prev.immobilisation === curr.immobilisation &&
          prevMontantAchatTTC === currMontantAchatTTC &&
          prevDateDebutISO === currDateDebutISO &&
          prev.periodeEnMois === curr.periodeEnMois &&
          prev.avanceMoyenPaiement?.id === curr.avanceMoyenPaiement?.id &&
          prev.avanceMoyenPaiement?.type === curr.avanceMoyenPaiement?.type &&
          prevValeurResiduelle === currValeurResiduelle;
      }),
      takeUntil(this.componentDestroyed$)
    ).subscribe(contract => {
      this.avanceChoisie = contract.avanceMoyenPaiement ?? null;
      const currentFormValue = this.formGroup.getRawValue(); // Use getRawValue to include disabled controls
      const patchObject: {
        numeroContrat?: string | null,
        immobilisation?: string | null,
        montantAchatTTC?: number | null,
        dateDebut?: Date | null,
        periodeEnMois?: number | null,
        valeurResiduelle?: number | null
      } = {};

      if (currentFormValue.numeroContrat !== contract.numeroContrat) {
        patchObject.numeroContrat = contract.numeroContrat;
      }
      if (currentFormValue.immobilisation !== contract.immobilisation) {
        patchObject.immobilisation = contract.immobilisation;
      }

      const contractMontantAchatTTCNum = contract.montantAchatTTC;
      if (currentFormValue.montantAchatTTC !== contractMontantAchatTTCNum) {
        patchObject.montantAchatTTC = contractMontantAchatTTCNum;
      }

      const currentFormDate = currentFormValue.dateDebut ? toLocalDateString(currentFormValue.dateDebut)! : null;
      const contractDate = contract.dateDebut ? toLocalDateString(contract.dateDebut) : null;
      if (currentFormDate !== contractDate) {
        patchObject.dateDebut = contract.dateDebut;
      }

      if (currentFormValue.periodeEnMois !== contract.periodeEnMois) {
        patchObject.periodeEnMois = contract.periodeEnMois;
      }

      const contractValResiduelleNum =  contract.valeurResiduelle;
      if (currentFormValue.valeurResiduelle !== contractValResiduelleNum) {
        patchObject.valeurResiduelle = contractValResiduelleNum;
      }

      if (Object.keys(patchObject).length > 0) {
        // Use { emitEvent: false } to prevent this patch from triggering valueChanges subscription below
        this.formGroup.patchValue(patchObject, { emitEvent: false });
      } else {
      }
      // ALWAYS recalculate on service update to ensure consistency
      this.calculateValeurResiduelle();
      this.formGroup.updateValueAndValidity();
    });
    this.subscriptions.push(contractSub);

    // NEW: Subscribe directly to montantAchatTTCControl valueChanges for immediate calculation
    const montantAchatSub = this.montantAchatTTCControl.valueChanges.pipe(
      // Optional: Add debounceTime if you want to wait a tiny bit before recalculating (e.g., 50ms)
      // This can prevent rapid, unnecessary calculations while the user is actively typing.
      // For immediate feel, you might not want debounceTime here, or a very small one.
      // debounceTime(50),
      distinctUntilChanged(), // Only trigger if montantAchatTTC actually changes
      takeUntil(this.componentDestroyed$)
    ).subscribe(() => {
      this.calculateValeurResiduelle();
      // No need to update the service here, the main valueChangeSub will handle that.
      // The goal here is purely immediate UI update for valeurResiduelle.
    });
    this.subscriptions.push(montantAchatSub);


    // 2. Subscribe to local form changes to update central state
    const valueChangeSub = this.formGroup.valueChanges.pipe(
      // Add distinctUntilChanged here to prevent updating service if form value hasn't truly changed
      debounceTime(200), // Add a debounceTime to prevent too frequent service updates
      distinctUntilChanged((prev, curr) => {
        // Compare only the relevant fields, converting to string or null consistently
        const prevMontantAchatTTC = prev.montantAchatTTC !== null && prev.montantAchatTTC !== undefined ? new Decimal(prev.montantAchatTTC).toString() : null;
        const currMontantAchatTTC = curr.montantAchatTTC !== null && curr.montantAchatTTC !== undefined ? new Decimal(curr.montantAchatTTC).toString() : null;

        const prevValeurResiduelle = prev.valeurResiduelle !== null && prev.valeurResiduelle !== undefined ? new Decimal(prev.valeurResiduelle).toString() : null;
        const currValeurResiduelle = curr.valeurResiduelle !== null && curr.valeurResiduelle !== undefined ? new Decimal(curr.valeurResiduelle).toString() : null;

        const prevDateDebutISO = prev.dateDebut ? toLocalDateString(prev.dateDebut) : null;
        const currDateDebutISO = curr.dateDebut ? toLocalDateString(curr.dateDebut) : null;

        return prev.numeroContrat === curr.numeroContrat &&
          prev.immobilisation === curr.immobilisation &&
          prevMontantAchatTTC === currMontantAchatTTC &&
          prevDateDebutISO === currDateDebutISO &&
          prev.periodeEnMois === curr.periodeEnMois &&
          prevValeurResiduelle === currValeurResiduelle;
      }),
      takeUntil(this.componentDestroyed$)
    ).subscribe(value => {

      // Safely convert form values to Decimal before updating service
      const montantAchatTTCForService = value.montantAchatTTC
      const valeurResiduelleForService = this.valeurResiduelleControl.value


      this.wizardService.updateContract({
        numeroContrat: value.numeroContrat,
        immobilisation: value.immobilisation,
        montantAchatTTC: montantAchatTTCForService,
        dateDebut: value.dateDebut,
        periodeEnMois: value.periodeEnMois,
        valeurResiduelle: valeurResiduelleForService
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

    // Emit initial validity state immediately on init
    this.stepValid.emit(this.formGroup.valid);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.componentDestroyed$.next();
    this.componentDestroyed$.complete();
  }

  // ── Avance : liste déroulante à chargement progressif ───────────────────────

  /** Recherche par numéro, côté serveur : la liste est rechargée avec les seuls moyens de paiement correspondants. */
  rechercherMoyen(event: { filter?: string }): void {
    this.recherche = (event.filter ?? '').trim();
    this.rechargerMoyens();
  }

  /** À la fermeture de la liste, la zone de recherche se vide : on retrouve la liste complète. */
  reinitialiserRecherche(): void {
    if (this.recherche) {
      this.recherche = '';
      this.rechargerMoyens();
    }
  }

  /** Retire l'avance du contrat. */
  retirerAvance(): void {
    this.wizardService.updateContract({ avanceMoyenPaiement: null });
  }

  /** Demandes de l'élément défilant : charge les pages correspondant aux lignes visibles. */
  chargerMoyens(event: { first: number; last: number }): void {
    const taille = GeneralDetailsStepComponent.TAILLE_PAGE;
    for (let page = Math.floor(event.first / taille); page <= Math.floor(event.last / taille); page++) {
      this.chargerPage(page);
    }
  }

  private rechargerMoyens(): void {
    this.pagesChargees.clear();
    this.moyens = [];
    this.chargerPage(0);
  }

  private chargerPage(page: number): void {
    const type = this.typeMoyenAvanceControl.value as TypeMoyenPaiementAvance | null;
    if (!type || this.pagesChargees.has(page)) {
      return;
    }
    this.pagesChargees.add(page);
    this.chargementMoyens = true;
    const taille = GeneralDetailsStepComponent.TAILLE_PAGE;
    this.echeanceLeasingService.getMoyensPaiementDisponibles(
      type, this.recherche, this.wizardService.getCurrentContract().id ?? null, page, taille
    ).pipe(takeUntil(this.componentDestroyed$)).subscribe({
      next: res => {
        const total = res.page.totalElements;
        const options = this.moyens.length === total ? [...this.moyens] : Array.from<MoyenPaiementAvance | undefined>({ length: total });
        res.content.forEach((moyen, i) => options[page * taille + i] = moyen);
        this.moyens = options;
        this.chargementMoyens = false;
      },
      error: () => {
        this.pagesChargees.delete(page);
        this.chargementMoyens = false;
      }
    });
  }

  //  calculate valeurResiduelle
  private calculateValeurResiduelle(): void {
    const montantAchatTTC = this.montantAchatTTCControl.value;
    const valeurResiduelleControl = this.valeurResiduelleControl;

    // Ensure montantAchatTTC is a valid number before calculation
    if (valeurResiduelleControl && typeof montantAchatTTC === 'number' && montantAchatTTC !== null && montantAchatTTC !== undefined) {
      const calculatedValue = new Decimal(montantAchatTTC).times(0.01).toDecimalPlaces(2).toNumber();
      // Only update if the value has actually changed to prevent unnecessary Angular change detection cycles
      if (valeurResiduelleControl.value !== calculatedValue) {
        // Emit change for UI update, but avoid triggering valueChanges for the formGroup
        // The disabled status of the control means it won't trigger statusChanges for formGroup either
        valeurResiduelleControl.setValue(calculatedValue, { emitEvent: false });
      }
    } else if (valeurResiduelleControl && valeurResiduelleControl.value !== null) {
      // Clear valeurResiduelle if montantAchatTTC is not a valid number
      valeurResiduelleControl.setValue(null, { emitEvent: false });
    }
  }


  get numeroContratControl(): FormControl {
    return this.formGroup.get('numeroContrat') as FormControl;
  }

  get immobilisationControl(): FormControl {
    return this.formGroup.get('immobilisation') as FormControl;
  }

  get montantAchatTTCControl(): FormControl {
    return this.formGroup.get('montantAchatTTC') as FormControl;
  }

  get dateDebutControl(): FormControl {
    return this.formGroup.get('dateDebut') as FormControl;
  }

  get periodeEnMoisControl(): FormControl {
    return this.formGroup.get('periodeEnMois') as FormControl;
  }

  get valeurResiduelleControl(): FormControl {
    return this.formGroup.get('valeurResiduelle') as FormControl;
  }

  get typeMoyenAvanceControl(): FormControl {
    return this.formGroup.get('typeMoyenAvance') as FormControl;
  }

  get moyenAvanceControl(): FormControl {
    return this.formGroup.get('moyenAvance') as FormControl;
  }

  markFormAsTouched(): void {
    this.formGroup.markAllAsTouched();
    this.stepValid.emit(this.formGroup.valid); // Re-emit validity after marking touched
  }
}
