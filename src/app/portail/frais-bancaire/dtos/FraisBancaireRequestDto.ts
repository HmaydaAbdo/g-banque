import {Decimal} from "decimal.js";

export interface FraisBancaireRequestDto {
  montant: Decimal;
  banqueId: number;
  dateEmission: Date;
  libelle?: string;
  observation?: string;
}
