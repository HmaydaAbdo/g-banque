import {TransactionSource} from "../transactions/TransactionSource";

export interface LcnTransactionRequest {
  numeroLcn: string;
  transactionSource: TransactionSource;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
}
