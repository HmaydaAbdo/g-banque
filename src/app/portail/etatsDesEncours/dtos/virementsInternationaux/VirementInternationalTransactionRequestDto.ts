import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface VirementInternationalTransactionRequestDto {
  id: number;
  transactionSource?: TransactionSource.VIREMENT_INTERNATIONAL;
  dateEmission: Date
  dateValeur: Date
  dateOperation: Date
  observation: string;
  libelle: string;
  tva: number;
}
