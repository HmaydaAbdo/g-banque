import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";


export interface RecaputilatifItem {
  id:number;
  dateEmission: string;
  dateEcheance: string;
  libelle: string;
  /** Numéro de pièce porté par la source ; `null` quand la source n'en a pas. */
  numero: string | null;
  sens: SensOperation;
  debit: Decimal;
  credit: Decimal;
  observations: string;
  source:TransactionSource
}
