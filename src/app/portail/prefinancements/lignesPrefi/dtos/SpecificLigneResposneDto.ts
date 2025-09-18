import {Decimal} from "decimal.js";

export interface PrefiResponseDto {

  id: number;
  marcheNumero: string;
  banqueName: string;
  montant: Decimal;
  encours: Decimal;
  solde: Decimal;
  taux: Decimal;
  dateDepart:Date;
  dateFin:Date;
  natureLigneFonctionnelleLabel:string;
  actif:boolean;
}
