import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";


export interface RecaputilatifItem {
  id:number;
  dateEmission: string;
  dateEcheance: string;
  libelle: string;
  sens: SensOperation;
  debit: Decimal;
  credit: Decimal;
  observations: string;
  source:TransactionSource
}
