import {Decimal} from "decimal.js";

export interface SpecificLigneResponseDto {
  id: number;
  marcheNumero: string;
  banqueNom: string;
  montant: Decimal;
  encours: Decimal;
  solde: Decimal;
  taux: number;
  dateDepart: string;
  dateFin: string;
  natureLigneFonctionnelleLabel: string;
  actif: boolean;
}
