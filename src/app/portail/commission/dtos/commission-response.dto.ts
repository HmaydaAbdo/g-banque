import {TransactionSource} from "../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";
import {Decimal} from "decimal.js";

export interface CommissionResponseDto {
  id: number;
  libelle: string;
  observation?: string;
  montant: Decimal;
  banqueId: number;
  banqueNom: string;
  operation: TransactionSource;
  dateEmission: Date;
  dateValeur?: Date;
  dateOperation?: Date;
  tvaDeductible: Decimal;
  status: Status;
}
