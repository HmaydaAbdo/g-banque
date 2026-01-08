
import { Decimal } from 'decimal.js';
import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface ChequeTransactionResponseDto {
  id: number;
  banqueName: string;
  chequeNumber: string;
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
