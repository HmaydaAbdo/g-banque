import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface LcnTransactionRequest {
  id: number;
  transactionSource?: TransactionSource;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
