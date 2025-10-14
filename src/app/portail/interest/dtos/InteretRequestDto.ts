import {Decimal} from "decimal.js";
import {TransactionSource} from "../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";

export interface InteretRequestDto {
  montant: Decimal;
  banqueId: number;
  dateEmission: Date;
  libelle?: string;
  observation?: string;
}
