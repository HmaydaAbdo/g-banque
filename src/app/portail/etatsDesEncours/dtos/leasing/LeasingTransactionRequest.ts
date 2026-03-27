import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../transactions/dtos/TransactionSource";


export interface LeasingTransactionRequest {

  contratLeasingId:number
  echeanceId:number
  transactionSource:TransactionSource
  dateEmission: string;       // Date d'émission
  dateValeur: string;         // Date de valeur
  dateOperation: string;      // Date de l'opération
  observation: string;       // Commentaire
  libelle:string;
  tvaDeductible: number;      // Montant de TVA déductible
}
