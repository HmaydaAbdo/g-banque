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
}
