import {Type} from "../Type";

export interface VirementSearchDto {
  numero?: string;
  dateEmission?: Date;
  montant?: number;
  beneficiaire?: string;
  banqueBeneficiaire?: string;
  nomBanqueEmettrice?: string;
  ribBeneficiaire?: string;
  active?: boolean | null;
  type?:Type
  emetteur?:string
}
