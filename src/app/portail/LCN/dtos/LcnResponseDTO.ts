import {Type} from "../../Virements/Type";
import {NatureLcn} from "./NatureLcn";

export interface LcnResponseDTO {
  numero: string;
  id:number
  type:Type
  dateEmission: string;
  beneficiaire: string;
  dateEcheance: string;
  dateOperation:string
  dateValeur:string
  libelle:string
  observation:string
  montant: number;
  active: boolean;
  banqueName: string;
  banqueId:number
  emetteur:string
  natureLcn:NatureLcn
}
