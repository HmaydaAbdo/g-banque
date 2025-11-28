import {Type} from "../../Virements/Type";

export interface ChequeResponseDto {
  id:number
  type:Type
  emetteur:string
  numero: string;
  dateEmission: string;
  dateEcheance: string;
  beneficiaire: string;
  montant: number;
  active: boolean;
  nomBanque: string;
  banqueId:number
  dateOperation:string;
  dateValeur:string;
  libelle:string;
  observation:string
}



