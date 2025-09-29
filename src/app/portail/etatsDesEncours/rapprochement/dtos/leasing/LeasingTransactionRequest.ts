import {Decimal} from "decimal.js";
import {TransactionSource} from "../transactions/TransactionSource";


export interface LeasingTransactionRequest {

  contratLeasingId:number
  echeanceId:number
  transactionSource:TransactionSource
  dateEmission: Date;       // Date d'émission
  dateValeur: Date;         // Date de valeur
  dateOperation: Date;      // Date de l'opération
  observation: string;       // Commentaire
  libelle:string;
  tvaDeductible: number;      // Montant de TVA déductible
}
