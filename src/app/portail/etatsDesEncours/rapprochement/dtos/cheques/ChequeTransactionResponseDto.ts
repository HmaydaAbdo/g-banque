
import { Decimal } from 'decimal.js';
import {TransactionSource} from "../transactions/TransactionSource";

export interface ChequeTransactionResponseDto {
  id: number;
  banqueName: string;
  chequeNumber: string;
  libelle: string;
  observation: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  tvaDeductible: number;
  tvaCollectee: number;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource;
}
