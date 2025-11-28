import {TransactionSource} from "../transactions/TransactionSource";

export interface ChequeTransactionRequest {
  idCheque: number;
  transactionSource: TransactionSource;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
