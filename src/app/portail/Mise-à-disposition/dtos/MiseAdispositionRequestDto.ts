import {Type} from "../../Virements/Type";

export interface MiseAdispositionRequestDto {
  banqueId:number
  beneficiaire:string
  dateEmission:Date
  emetteur:string
  montant:number
  type:Type
}
