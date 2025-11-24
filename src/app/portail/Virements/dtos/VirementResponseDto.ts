
import {Type} from "../Type";

export interface VirementResponseDto {
  numero: string;
  dateEmission: string;
  beneficiaire: string;
  montant: number;
  banqueEmettriceNom: string;
  banqueBeneficiaire: string;
  ribBeneficiaire: string;
  dateOperation:string;
  dateValeur:string;
  libelle:string;
  observation:string;
  active: boolean;
  type:Type
}
