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
  emetteur?: string;
  statut?: StatutMoyenPaiement;

  /** @deprecated Utilisé uniquement par les écrans d'encours (etatsDesEncours), qui partagent cette interface. */
  montant?: Decimal;
  /** @deprecated Utilisé uniquement par les écrans d'encours (etatsDesEncours), qui partagent cette interface. */
  dateEmission?: Date;

  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
}
