import {Decimal} from "decimal.js";

export interface PrelevementRequestDto {
  libelle: string;
  observation: string;
  montant: Decimal;
  banqueId: number;
  dateEmission: Date;
}
