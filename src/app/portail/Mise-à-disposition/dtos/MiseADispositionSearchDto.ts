import {Decimal} from "decimal.js";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

/**
 * Interface for the search criteria used to filter MiseADisposition entities.
 * All properties are optional as they are used for dynamic filtering.
 */
export interface MiseADispositionSearchDto {
  numero?: string;
  beneficiaire?: string;
  cinBeneficiaire?: string;
  banqueName?: string;
  montant?: Decimal;
  emetteur?:string
  dateEmission?: Date;
  statut?: StatutMoyenPaiement;
}
