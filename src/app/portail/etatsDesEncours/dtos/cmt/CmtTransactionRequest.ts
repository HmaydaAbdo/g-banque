import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface CmtTransactionRequest {

  cmtId:number
  echeanceId:number
  transactionSource:TransactionSource
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle:string;
  tvaDeductible: number;
  rapprochementId: number;
}
