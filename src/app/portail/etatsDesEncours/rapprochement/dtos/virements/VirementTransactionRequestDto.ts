import {TransactionSource} from "../../../../transactions/dtos/TransactionSource";

export interface VirementTransactionRequestDto {
  id: number;
  transactionSource: TransactionSource;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  tvaDeductible: number;
  libelle: string;
}
