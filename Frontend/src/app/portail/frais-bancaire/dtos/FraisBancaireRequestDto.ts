import {Decimal} from "decimal.js";

export interface FraisBancaireRequestDto {
  montant: Decimal;
  banqueId: number;
  dateEmission: string;
  libelle?: string;
  observation?: string;
}
