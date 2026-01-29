import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";


export interface RecaputilatifItem {
  id:number;
  dateEmission: string;
  dateEcheance: string;
  libelle: string;
  debit: Decimal;
  credit: Decimal;
  observations: string;
  source:TransactionSource
}
