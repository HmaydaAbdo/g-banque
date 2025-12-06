import {Decimal} from "decimal.js";
import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";

/**
 * Interface representing the consolidated disbursement item.
 * This corresponds to the Java DTO RecaputilatifItem.
 */
export interface RecaputilatifItem {
  // The disbursement date, transmitted as an ISO 8601 string from the API.
  dateEmission: string;
  dateEcheance: string;
  // A descriptive label for the disbursement, e.g., 'Contrat N°...'
  libelle: string;
  // The amount of the disbursement.
  debit: Decimal;
  credit: Decimal;
  // Additional observations or details.
  observations: string;
  source:TransactionSource
}
