import {Decimal} from "decimal.js";
import {TransactionSource} from "../transactions/TransactionSource";

export interface PrefinancementTransactionResponse {
  id: number;
  banqueName: string;
  marcheNumero: string;
  libelle: string;
  observation: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource
}
