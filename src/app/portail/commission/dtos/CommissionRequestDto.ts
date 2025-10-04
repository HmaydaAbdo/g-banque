import {TransactionSource} from "../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import {Decimal} from "decimal.js";

export interface CommissionRequestDto {
  libelle: string;
  observation: string;
  montant: Decimal;
  banqueId: number;
  operation: TransactionSource;
  dateEmission: Date;
}
