
import {Type} from "../Type";

export interface VirementResponseDto {
  id:number
  numero: string;
  dateEmission: string;
  beneficiaire: string;
  montant: number;
  banqueEmettriceNom: string;
  emetteur: string;
  banqueBeneficiaire: string;
  ribBeneficiaire: string;
  dateOperation:string;
  dateValeur:string;
  libelle:string;
  observation:string;
  active: boolean;
  type:Type
  banqueEmettriceId:number
  banqueBeneficiaireId:number
}
