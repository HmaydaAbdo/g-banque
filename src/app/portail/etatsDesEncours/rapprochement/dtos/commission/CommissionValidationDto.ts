import {Decimal} from "decimal.js";

export interface CommissionValidationDto {
  idCommission: number;
  dateEmission: Date;
  libelle: string;
  observation: string;
  dateValeur: Date;
  dateOperation: Date;
  tva:Decimal;
}
