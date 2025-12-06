import {TransactionSource} from "../../../../transactions/dtos/TransactionSource";
import {Decimal} from "decimal.js";

export interface CommissionTransactionResponse {
  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource;
}
