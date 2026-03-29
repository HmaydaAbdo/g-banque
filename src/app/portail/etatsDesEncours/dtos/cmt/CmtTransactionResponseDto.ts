import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {Decimal} from "decimal.js";

export interface CmtTransactionResponseDto {

  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  creditoMoyenTermeId: number;
  echeanceId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource;

}
