import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface DroitConstateTransactionResponse {

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
