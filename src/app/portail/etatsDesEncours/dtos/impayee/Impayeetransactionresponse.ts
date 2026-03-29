import { TransactionSource } from "../../../transactions/dtos/TransactionSource";

export interface ImpayeeTransactionResponse {
  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: number;
  tvaCollectee: number;
  debit: number;
  credit: number;
  transactionSource: TransactionSource;
}
