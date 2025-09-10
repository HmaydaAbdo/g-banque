import {TransactionSource} from "../transactions/TransactionSource";

export interface VirementTransactionRequestDto {
  numeroVirement: string;
  transactionSource: TransactionSource;
  dateEmission: Date;    // ISO 8601
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
}
