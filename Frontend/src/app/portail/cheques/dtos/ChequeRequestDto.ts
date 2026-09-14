import {Type} from "../../Virements/Type";


export  interface ChequeRequestDto {
  numero:string
  dateEmission: string
  dateEcheance: string
  beneficiaire:string
  banqueId:number
  emetteur:string
  montant:number
  type:Type
}
