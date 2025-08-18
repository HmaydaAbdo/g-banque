import {TransactionSource} from "./TransactionSource";
import {Decimal} from "decimal.js";

export interface CmtTransactionResponseDto {

  id: number;
  banqueName: string;
  libelle: string;
  observation: string;
  creditoMoyenTermeId: number;
  echeanceId: number;
  dateEmission: Date;
  dateValeur: Date;
  dateOperations: Date;
  tvaDeductible: number;
  tvaCollectee: number;
  debit: Decimal;
  credit: Decimal;
  transactionSource: TransactionSource;

}
