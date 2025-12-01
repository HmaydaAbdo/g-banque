import {TransactionSource} from "../transactions/TransactionSource";

export interface MiseAdispositionTransactionRequestDto {
  id: number;
  transactionSource: TransactionSource;
  dateEmission: Date;    // ISO 8601
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
