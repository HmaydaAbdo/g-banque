import {Decimal} from "decimal.js";

/**
 * Interface representing the consolidated disbursement item.
 * This corresponds to the Java DTO RécaputilatifDecaissementItem.
 */
export interface RecaputilatifDecaissementItem {
  // The disbursement date, transmitted as an ISO 8601 string from the API.
  dateEmission: string;

  // A descriptive label for the disbursement, e.g., 'Contrat N°...'
  libelle: string;

  // The amount of the disbursement.
  debit: Decimal;

  // Additional observations or details.
  observations: string;
}
