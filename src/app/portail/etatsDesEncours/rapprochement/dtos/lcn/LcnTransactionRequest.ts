import {TransactionSource} from "../transactions/TransactionSource";

export interface LcnTransactionRequest {
  id: number;
  transactionSource: TransactionSource;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
