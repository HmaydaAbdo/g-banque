import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface DecompteTransactionResponseDto {

  id: number;
  banqueName: string;
  marcheNumero: string;
  libelle: string;
  observation: string;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource
}
