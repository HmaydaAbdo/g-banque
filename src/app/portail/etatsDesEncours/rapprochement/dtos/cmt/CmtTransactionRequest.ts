import {TransactionSource} from "../transactions/TransactionSource";

export interface CmtTransactionRequest {

  cmtId:number
  echeanceId:number
  transactionSource:TransactionSource
  dateEmission: Date;       // Date d'émission
  dateValeur: Date;         // Date de valeur
  dateOperation: Date;      // Date de l'opération
  observation: string;       // Commentaire
  tvaDeductible: number;      // Montant de TVA déductible
}
