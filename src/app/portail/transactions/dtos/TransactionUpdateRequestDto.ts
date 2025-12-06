import {TransactionSource} from "./TransactionSource";


export interface TransactionUpdateRequestDto {
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  libelle: string;
  observation: string;
  tvaDeductible: number;
  tvaCollectee: number;
  transactionSource: TransactionSource;
  debit: number;
  credit: number;
  banqueId: number;
}
