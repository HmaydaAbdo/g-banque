import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {Decimal} from "decimal.js";


export interface LcnTransactionResponse {
  id: number;
  banqueName: string;
  lcnNumber: string;
  libelle: string;
  observation: string;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: number;
  credit: number;
  transactionSource: TransactionSource;
}
