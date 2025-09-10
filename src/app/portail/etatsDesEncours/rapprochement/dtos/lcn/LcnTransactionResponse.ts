import {TransactionSource} from "../transactions/TransactionSource";


export interface LcnTransactionResponse {
  id: number;
  banqueName: string;
  lcnNumber: string;
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
