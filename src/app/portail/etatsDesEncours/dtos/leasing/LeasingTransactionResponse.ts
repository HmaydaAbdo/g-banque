import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface LeasingTransactionResponse {
  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  contratLeasingId: number;
  echeanceId: number;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: Decimal;
  credit: Decimal;
  transactionSource:TransactionSource
}
