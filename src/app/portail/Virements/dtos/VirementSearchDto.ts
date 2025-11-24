import {Type} from "../Type";

export interface VirementSearchDto {
  numero: string;
  beneficiaire: string;
  banqueBeneficiaire: string;
  nomBanqueEmettrice: string;
  ribBeneficiaire: string;
  active: boolean | null;
  type:Type
}
