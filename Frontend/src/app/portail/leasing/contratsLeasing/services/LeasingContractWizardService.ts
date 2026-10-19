
import { Injectable, inject } from '@angular/core';
import {BehaviorSubject, Observable, throwError} from 'rxjs';
import {distinctUntilChanged, map, catchError} from 'rxjs/operators';
import {ContratLeasingRequestDTO, ContratLeasingResponseDTO} from '../dtos/contrat-leasing';
import { Decimal } from 'decimal.js';
import {ContratLeasingPhaseRequestDTO} from "../dtos/contrat-leasing-phase";
import {TypeContratLeasing} from "../dtos/type-contrat-leasing";
import {SubmissionSuccessResponse} from "../dtos/SubmissionSuccessResponse";
import {ContratLeasingService} from "./contrat-leasing.service";

/**
 * Aperçu en direct du plan du contrat (panneau latéral du wizard) : décomposition avance / mensualités / valeur
 * résiduelle, endettement et dates clés. Calculé uniquement à partir de l'état du wizard.
 */
export interface ApercuContrat {
  achat: Decimal | null;
  avance: Decimal | null;
  /** Mois couverts par les phases de mensualités. */
  moisMensualites: number;
  totalMensualites: Decimal;
  valeurResiduelle: Decimal | null;
  /** Avance + mensualités + valeur résiduelle. */
  endettement: Decimal;
  /** Endettement moins prix d'achat : coût du financement (null tant que le prix d'achat n'est pas saisi). */
  coutFinancement: Decimal | null;
  /** Mois que les phases doivent couvrir : période moins le mois de l'avance. */
  dureeACouvrir: number | null;
  dateAvance: Date | null;
  datePremiereMensualite: Date | null;
  dateValeurResiduelle: Date | null;
}

/** Ajoute des mois en ramenant le jour à la fin du mois si besoin (comme LocalDate.plusMonths côté serveur). */
function ajouterMois(date: Date, mois: number): Date {
  const cible = new Date(date.getFullYear(), date.getMonth() + mois, 1);
  const dernierJour = new Date(cible.getFullYear(), cible.getMonth() + 1, 0).getDate();
  cible.setDate(Math.min(date.getDate(), dernierJour));
  return cible;
}

// Define the initial empty contract state
const initialContract: ContratLeasingRequestDTO = {
  numeroContrat: null,
  immobilisation: null,
  montantAchatTTC: null,
  dateDebut: null,
  periodeEnMois: null,
  organismeLeasingId: null,
  banqueDePrelevementId: null,
  typeContrat: null,
  valeurResiduelle: null,
  avanceTTC: null,
  phases: []
};

@Injectable({
  providedIn: 'root'
})
export class LeasingContractWizardService {

  private _contract = new BehaviorSubject<ContratLeasingRequestDTO>(initialContract);
  contract$: Observable<ContratLeasingRequestDTO> = this._contract.asObservable();

  private _globalValidationErrors = new BehaviorSubject<string[]>([]);
  globalValidationErrors$: Observable<string[]> = this._globalValidationErrors.asObservable();

  private _submissionError = new BehaviorSubject<string | null>(null);
  submissionError$: Observable<string | null> = this._submissionError.asObservable();

  //  Observable for expectedTotalAmount (L'Indemnité)
  // It will automatically update whenever the contract$ changes
  public expectedTotalAmount$: Observable<Decimal | null>;

  /** Aperçu en direct du plan (avance, mensualités, valeur résiduelle, endettement, dates clés). */
  public apercu$: Observable<ApercuContrat>;


  private contratLeasingService = inject(ContratLeasingService);

  constructor() {
    this.contract$.pipe(
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(contract => {
      this.validateContract(contract);
    });

    // Initialize expectedTotalAmount$
    this.expectedTotalAmount$ = this.contract$.pipe(
      map(contract => {
        let totalMensualities = new Decimal(0);

        // Sum (duree * mensualité) for all phases
        for (const phase of contract.phases) {
          const duree = new Decimal(phase.dureeMois || 0); // Treat null/undefined dureeMois as 0
          const mensualite = phase.mensualiteTTC || new Decimal(0); // Treat null/undefined mensualiteTTC as 0
          totalMensualities = totalMensualities.plus(duree.times(mensualite));

        }

        // Add valeurResiduelle
        const valeurResiduelle = contract.valeurResiduelle || new Decimal(0); // Treat null/undefined valeurResiduelle as 0
        const avance = new Decimal(contract.avanceTTC || 0);
        const expectedTotal = totalMensualities.plus(valeurResiduelle).plus(avance);

        return expectedTotal;
      }),
      distinctUntilChanged((prev, curr) => prev?.eq(curr || new Decimal(0))) // Prevent emission if value hasn't numerically changed
    );

    this.apercu$ = this.contract$.pipe(map(contract => this.calculerApercu(contract)));
  }

  private calculerApercu(contract: ContratLeasingRequestDTO): ApercuContrat {
    const avance = contract.avanceTTC ? new Decimal(contract.avanceTTC) : null;
    const achat = contract.montantAchatTTC ? new Decimal(contract.montantAchatTTC) : null;
    const valeurResiduelle = contract.valeurResiduelle != null ? new Decimal(contract.valeurResiduelle) : null;

    let totalMensualites = new Decimal(0);
    let moisMensualites = 0;
    for (const phase of contract.phases) {
      const duree = phase.dureeMois || 0;
      moisMensualites += duree;
      totalMensualites = totalMensualites.plus(new Decimal(phase.mensualiteTTC || 0).times(duree));
    }

    const endettement = totalMensualites.plus(valeurResiduelle ?? 0).plus(avance ?? 0);
    const debut = contract.dateDebut ? new Date(contract.dateDebut) : null;
    const periode = contract.periodeEnMois;

    return {
      achat,
      avance,
      moisMensualites,
      totalMensualites,
      valeurResiduelle,
      endettement,
      coutFinancement: achat && endettement.gt(0) ? endettement.minus(achat) : null,
      dureeACouvrir: periode ? periode - (avance ? 1 : 0) : null,
      dateAvance: avance ? debut : null,
      datePremiereMensualite: debut ? ajouterMois(debut, avance ? 1 : 0) : null,
      dateValeurResiduelle: debut && periode ? ajouterMois(debut, periode) : null
    };
  }

  getCurrentContract(): ContratLeasingRequestDTO {
    return this._contract.getValue();
  }

  updateContract(updates: Partial<ContratLeasingRequestDTO>): void {
    const currentContract = this._contract.getValue();
    const updatedContract = { ...currentContract, ...updates };

    if (updates.montantAchatTTC != null) {
      updatedContract.montantAchatTTC = updates.montantAchatTTC;
    }
    if (updates.valeurResiduelle != null) {
      updatedContract.valeurResiduelle = updates.valeurResiduelle;
    }

    this._contract.next(updatedContract);
  }

  addPhase(phase: ContratLeasingPhaseRequestDTO): void {
    const currentContract = this.getCurrentContract();
    const updatedPhases = [...currentContract.phases];
    updatedPhases.push({
      ...phase,
      // Ensure mensualiteTTC is a Decimal when adding
      mensualiteTTC: new Decimal(phase.mensualiteTTC)
    });
    this.updateContract({ phases: updatedPhases });
  }

  updatePhase(index: number, updatedPhase: ContratLeasingPhaseRequestDTO): void {
    const currentContract = this.getCurrentContract();
    const updatedPhases = [...currentContract.phases];
    if (index >= 0 && index < updatedPhases.length) {
      updatedPhases[index] = {
        ...updatedPhase,
        // Ensure mensualiteTTC is a Decimal when updating
        mensualiteTTC: new Decimal(updatedPhase.mensualiteTTC)
      };
      this.updateContract({ phases: updatedPhases });
    }
  }

  removePhase(index: number): void {
    const currentContract = this.getCurrentContract();
    const updatedPhases = currentContract.phases.filter((_, i) => i !== index);

    const renumberedPhases = updatedPhases.map((phase, i) => ({
      ...phase,
      numeroPhase: i + 1
    }));

    this.updateContract({ phases: renumberedPhases });
  }

  clearPhases(): void {
    this.updateContract({ phases: [] });
  }

  submitContractToBackend(): Observable<SubmissionSuccessResponse> {
    const finalContract = this.getCurrentContract();

    // Clear any previous submission errors before a new attempt
    this._submissionError.next(null);

    let submissionObservable: Observable<ContratLeasingResponseDTO>;

    // Determine if it's a creation or an update based on the presence of an ID
    if (finalContract.id !== null && finalContract.id !== undefined) {
      submissionObservable = this.contratLeasingService.updateContratLeasing(finalContract.id, finalContract);
    } else {
      submissionObservable = this.contratLeasingService.createContratLeasing(finalContract);
    }

    return submissionObservable.pipe(
      map(response => {
        // On successful submission, clear any existing submission error
        this._submissionError.next(null);
        return {
          message: finalContract.id ? 'Contrat mis à jour avec succès!' : 'Contrat soumis avec succès!',
          contractId: response.id.toString()
        };
      }),
      catchError(error => {
        // On error: set the submission error state and re-throw the error
        let errorMessage = 'Une erreur inattendue est survenue lors de la soumission.';
        if (error instanceof Error) {
          errorMessage = error.message; // Use the message propagated from handleError in ContratLeasingService
        }
        this._submissionError.next(errorMessage);
        return throwError(() => new Error(errorMessage)); // Re-throw for the component to handle
      })
    );
  }
  //  Method to reset the wizard's contract state
  resetContract(): void {
    this._contract.next(initialContract);
    this._globalValidationErrors.next([]); // Clear global errors on reset
    this._submissionError.next(null);
  }

  // --- Global Validation ---
  private validateContract(contract: ContratLeasingRequestDTO): void {
    const errors: string[] = [];

    // Rule 1: Phases required for non-linear contracts
    if (contract.typeContrat && (!contract.phases || contract.phases.length === 0)) {
      errors.push('phases:Des phases sont requises pour le type de contrat ' + this.getContractTypeDisplayName(contract.typeContrat) + '.');
    }

    // Rule 2: Total duration of phases must match contract period, minus the avance month (if period is set)
    if (contract.periodeEnMois !== null && contract.periodeEnMois !== undefined && contract.periodeEnMois > 0) {
      const dureeACouvrir = contract.periodeEnMois - (contract.avanceTTC ? 1 : 0);
      const totalPhasesDuration = contract.phases.reduce((sum, phase) => sum + (phase.dureeMois || 0), 0);
      if (totalPhasesDuration !== dureeACouvrir) {
        const precision = contract.avanceTTC
          ? `période de ${contract.periodeEnMois} mois moins le mois de l'avance`
          : 'période du contrat';
        errors.push(`phases:La durée totale des phases (${totalPhasesDuration} mois) ne correspond pas à la durée à couvrir (${dureeACouvrir} mois, soit ${precision}).`);
      }
    }

    // Avance : incluse dans le prix d'achat (donc strictement inférieure) et compte pour un mois de la période
    if (contract.avanceTTC != null) {
      if (contract.avanceTTC <= 0) {
        errors.push("general-details:L'avance doit être supérieure à zéro.");
      } else if (contract.montantAchatTTC != null && contract.avanceTTC >= contract.montantAchatTTC) {
        errors.push("general-details:L'avance doit être strictement inférieure au montant d'achat TTC.");
      }
      if (contract.periodeEnMois != null && contract.periodeEnMois < 2) {
        errors.push("general-details:Un contrat avec avance doit durer au moins 2 mois (1 mois d'avance et au moins 1 mois de mensualités).");
      }
    }

    // Rule 3: Phase mensualities order for Progressive/Degressive
    if (contract.typeContrat === TypeContratLeasing.PROGRESSIF && contract.phases.length > 1) {
      for (let i = 0; i < contract.phases.length - 1; i++) {
        const current = contract.phases[i].mensualiteTTC;
        const next = contract.phases[i + 1].mensualiteTTC;
        if (current && next && current.greaterThanOrEqualTo(next)) {
          errors.push('phases:L\'ordre des mensualités est invalide pour un contrat Progressif (elles doivent augmenter).');
          break;
        }
      }
    }

    if (contract.typeContrat === TypeContratLeasing.DEGRESSIF && contract.phases.length > 1) {
      for (let i = 0; i < contract.phases.length - 1; i++) {
        const current = contract.phases[i].mensualiteTTC;
        const next = contract.phases[i + 1].mensualiteTTC;
        if (current && next && current.lessThanOrEqualTo(next)) {
          errors.push('phases:L\'ordre des mensualités est invalide pour un contrat Dégressif (elles doivent diminuer).');
          break;
        }
      }
    }
    if (contract.montantAchatTTC == null || contract.montantAchatTTC <= 0) {
      errors.push('montantAchatTTC:Le Montant d\'achat TTC est requis et doit être positif.');
    }
    if (contract.valeurResiduelle == null || contract.valeurResiduelle < 0) {
      errors.push('valeurResiduelle:La Valeur Résiduelle est requise et doit être positive ou nulle.');
    }

    this._globalValidationErrors.next(errors);
  }

  private getContractTypeDisplayName(type: TypeContratLeasing | null | undefined): string {
    if (type === null || type === undefined) return 'Inconnu';
    switch (type) {
      case TypeContratLeasing.LINEAIRE: return 'Linéaire';
      case TypeContratLeasing.PROGRESSIF: return 'Progressif';
      case TypeContratLeasing.DEGRESSIF: return 'Dégressif';
      default: return 'Inconnu';
    }
  }

  loadContractForEdit(contract: ContratLeasingRequestDTO): void {
    const contractToLoad: ContratLeasingRequestDTO = {
      ...contract,
      montantAchatTTC: contract.montantAchatTTC ? contract.montantAchatTTC: null,
      valeurResiduelle: contract.valeurResiduelle ? contract.valeurResiduelle : null,
      phases: contract.phases.map(p => ({
        ...p,
        mensualiteTTC: p.mensualiteTTC ? new Decimal(p.mensualiteTTC) : new Decimal(0)
      }))
    };
    this._contract.next(contractToLoad);
  }
}
