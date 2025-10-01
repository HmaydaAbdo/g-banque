import {TransactionSource} from "../transactions/TransactionSource";

export interface MiseAdispositionTransactionRequestDto {
  numeroMiseAdisposition: string;
  transactionSource: TransactionSource;
  dateEmission: Date;    // ISO 8601
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
