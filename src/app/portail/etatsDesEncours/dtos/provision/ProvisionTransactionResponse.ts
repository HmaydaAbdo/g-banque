import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../transactions/dtos/SensOperation";

export interface ProvisionTransactionResponse {
  id: number;
  provisionId: number;
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
  transactionSource: TransactionSource.PROVISION;
  sens: SensOperation;
}
