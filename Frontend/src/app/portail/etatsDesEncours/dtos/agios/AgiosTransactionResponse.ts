import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {Decimal} from "decimal.js";

export interface AgiosTransactionResponse {
  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource;
}
