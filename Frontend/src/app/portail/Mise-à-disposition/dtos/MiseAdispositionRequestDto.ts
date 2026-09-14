import {Type} from "../../Virements/Type";

export interface MiseAdispositionRequestDto {
  banqueId:number
  beneficiaire:string
  dateEmission:string
  emetteur:string
  montant:number
  type:Type
}
