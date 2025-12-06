import {Decimal} from "decimal.js";
import {TransactionSource} from "../../transactions/dtos/TransactionSource";

export interface InteretRequestDto {
  montant: Decimal;
  banqueId: number;
  dateEmission: Date;
  libelle?: string;
  observation?: string;
}
