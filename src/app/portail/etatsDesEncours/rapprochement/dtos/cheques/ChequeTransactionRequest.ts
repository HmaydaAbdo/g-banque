import {TransactionSource} from "../transactions/TransactionSource";

export interface ChequeTransactionRequest {
  numeroCheque: string;
  transactionSource: TransactionSource;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
}
