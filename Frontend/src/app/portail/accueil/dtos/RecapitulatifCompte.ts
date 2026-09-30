import {Devise} from "../../comptes-bancaire/enums/Devise";

export interface RecapitulatifCompte {
  banqueId:   number;
  nom:        string;
  logoBase64: string;
  soldeActuel:   number;
  soldeDebut:    number;
  plafondFC?:    number;
  /** Solde actuel + plafond FC (situation du jour) — toujours renseignée par le backend. */
  disponibiliteFC: number;
  devise:        Devise;
  lcnsEchusCount:      number;
  virementsEchusCount: number;


  // ── État prévisionnel de fin de mois (toujours renseignés par le backend) ──
  /** Total prévisionnel des encaissements jusqu'à fin de mois. */
  encoursPlus:  number;
  /** Total prévisionnel des décaissements jusqu'à fin de mois. */
  encoursMoins: number;
  /** soldeActuel + encoursPlus − encoursMoins. */
  soldeFinDeMois: number;
  /** soldeFinDeMois + plafond FC. */
  disponibiliteFinDeMois: number;
}
