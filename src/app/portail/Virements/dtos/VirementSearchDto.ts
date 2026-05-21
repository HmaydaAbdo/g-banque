import {Type} from "../Type";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

export interface VirementSearchDto {
  numero?: string;
  dateEmission?: Date;
  montant?: number;
  beneficiaire?: string;
  banqueBeneficiaire?: string;
  nomBanqueEmettrice?: string;
  ribBeneficiaire?: string;
  statut?: StatutMoyenPaiement | null;
  type?:Type
  emetteur?:string
}
