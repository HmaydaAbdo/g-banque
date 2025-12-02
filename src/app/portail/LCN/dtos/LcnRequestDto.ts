import {Type} from "../../Virements/Type";

export  interface LcnRequestDto {

  numero:string
  dateEmission:Date
  dateEcheance:Date
  beneficiaire:string
  banqueId:number
  emetteur:string
  montant:number
  type:Type
}
