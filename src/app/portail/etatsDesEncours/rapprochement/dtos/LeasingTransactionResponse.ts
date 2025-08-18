import {Decimal} from "decimal.js";
import {TransactionSource} from "./TransactionSource";

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
  tvaDeductible: number;
  tvaCollectee: number;
  debit: Decimal;
  credit: Decimal;
  transactionSource:TransactionSource
}
