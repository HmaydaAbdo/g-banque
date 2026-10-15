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

/** Une catégorie (ex. « Leasing ») rattachée à sa famille (ex. « Financements ») : un montant par colonne, et leur total. */
export interface LignePlan {
  famille: string;
  categorie: string;
  montants: number[];
  total: number;
}

/** Un des plus gros flux à venir : somme d'une catégorie, un jour, dans un sens. `montant` est positif. */
export interface Mouvement {
  date: string;
  categorie: string;
  famille: string;
  sens: 'ENCAISSEMENT' | 'DECAISSEMENT';
  montant: number;
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
  /** Devise de la banque, ou de toutes les banques d'un total (jamais de devises additionnées). */
  devise: string;
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
  /** Les cinq plus gros flux à venir, du plus gros au plus petit. */
  plusGrosMouvements: Mouvement[];
  /** Jour depuis lequel la banque est en besoin sans interruption (nul sans besoin, et pour un total). */
  enBesoinDepuis: string | null;
}

export interface PlanTresorerie {
  periode: PeriodeRecap;
  dateCalcul: string;
  dateHorizon: string;
  colonnes: ColonnePlan[];
  /** Un total par devise présente, le dirham en premier. */
  groupes: PlanBanque[];
  banques: PlanBanque[];
}
