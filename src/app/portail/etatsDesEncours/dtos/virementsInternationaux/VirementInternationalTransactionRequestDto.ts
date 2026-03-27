import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface VirementInternationalTransactionRequestDto {
  id: number;
  transactionSource?: TransactionSource.VIREMENT_INTERNATIONAL;
  dateEmission: string
  dateValeur: string
  dateOperation: string
  observation: string;
  libelle: string;
  tva: number;
}
