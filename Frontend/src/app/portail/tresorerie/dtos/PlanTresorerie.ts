import { PeriodeRecap } from '../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';

/**
 * Plan de trésorerie prévisionnel — miroir des DTO backend
 * staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto et associés.
 * Les dates sont des chaînes ISO (yyyy-MM-dd).
 */

/** Colonne du plan : « À ce jour » (code ECHU, `du` nul), puis semaines ou mois jusqu'à l'horizon. */
export interface ColonnePlan {
  code: string;
  libelle: string;
  du: string | null;
  au: string;
}

/** Une catégorie (ex. « Leasing ») : un montant par colonne, et leur total. */
export interface LignePlan {
  categorie: string;
  montants: number[];
  total: number;
}

export interface SoldeJour {
  date: string;
  solde: number;
}

export interface PointBas {
  date: string;
  solde: number;
  disponibilite: number;
}

/** Plan d'une banque, ou de toutes les banques consolidées (`banqueId` nul). */
export interface PlanBanque {
  banqueId: number | null;
  banqueNom: string;
  soldeActuel: number;
  plafondFC: number;
  encaissements: LignePlan[];
  decaissements: LignePlan[];
  totalEncaissements: number[];
  totalDecaissements: number[];
  /** Solde prévu à la fin de chaque colonne. */
  soldesFin: number[];
  /** soldesFin + plafond FC. */
  disponibilites: number[];
  soldeFinal: number;
  disponibiliteFinale: number;
  pointBas: PointBas;
  /** Ce qui manque au point bas une fois la facilité de caisse épuisée (0 si couvert). */
  besoinFinancement: number;
  joursSoldeNegatif: number;
  courbe: SoldeJour[];
}

export interface PlanTresorerie {
  periode: PeriodeRecap;
  dateCalcul: string;
  dateHorizon: string;
  colonnes: ColonnePlan[];
  consolide: PlanBanque;
  banques: PlanBanque[];
}
