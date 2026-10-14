import {Devise} from "../../comptes-bancaire/enums/Devise";
import {PeriodeRecap} from "../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap";

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


  // ── Projection à l'horizon demandé (toujours renseignée par le backend) ──
  /** Horizon de la projection : fin de mois (défaut) ou fin de trimestre. */
  periode: PeriodeRecap;
  /** Dernier jour inclus dans la projection (yyyy-MM-dd). */
  dateHorizon: string;
  /** Total prévisionnel des encaissements jusqu'à l'horizon. */
  encoursPlus:  number;
  /** Total prévisionnel des décaissements jusqu'à l'horizon. */
  encoursMoins: number;
  /** soldeActuel + encoursPlus − encoursMoins. */
  soldePrevu: number;
  /** soldePrevu + plafond FC. */
  disponibilitePrevue: number;
}
