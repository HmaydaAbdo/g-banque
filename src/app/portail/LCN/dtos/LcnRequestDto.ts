import {Type} from "../../Virements/Type";

export  interface LcnRequestDto {

  dateEmission:Date
  dateEcheance:Date
  beneficiaire:string
  banqueId:number
  emetteur:string
  montant:number
  type:Type
}
