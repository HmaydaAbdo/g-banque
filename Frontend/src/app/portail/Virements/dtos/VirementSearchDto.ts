import {Type} from "../Type";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

export interface VirementSearchDto {
  numero?: string;
  montant?: number;
  beneficiaire?: string;
  banqueBeneficiaire?: string;
  nomBanqueEmettrice?: string;
  ribBeneficiaire?: string;
  statut?: StatutMoyenPaiement | null;
  type?: Type;
  emetteur?: string;

  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
}
