import {Decimal} from "decimal.js";

export interface CommissionValidationDto {
  idCommission: number;
  dateEmission: string;
  libelle: string;
  observation: string;
  dateValeur: string;
  dateOperation: string;
  tva:Decimal;
  rapprochementId: number;
}
