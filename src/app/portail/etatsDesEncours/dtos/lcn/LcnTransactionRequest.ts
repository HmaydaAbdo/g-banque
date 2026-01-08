import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

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
