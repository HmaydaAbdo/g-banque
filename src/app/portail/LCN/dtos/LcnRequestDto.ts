import {Type} from "../../Virements/Type";
import {NatureLcn} from "./NatureLcn";

export  interface LcnRequestDto {

  numero:string
  dateEmission:Date
  dateEcheance:Date
  beneficiaire:string
  banqueId:number
  emetteur:string
  montant:number
  type:Type
  natureLcn:NatureLcn
}
