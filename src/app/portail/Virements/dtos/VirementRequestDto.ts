import {Type} from "../Type"

export interface VirementRequestDto {

  banqueBeneficiaireId:number
  dateReception:Date
  beneficiaire:string
  montant:number
  emetteur:string
  type:Type

}
