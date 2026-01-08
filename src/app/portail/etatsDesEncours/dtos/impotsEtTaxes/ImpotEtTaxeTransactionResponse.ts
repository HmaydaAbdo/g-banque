import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface ImpotEtTaxeTransactionResponse {
  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  tvaDeductible: number;
  tvaCollectee: number;
  debit: number;
  credit: number;
  transactionSource: TransactionSource;
}
