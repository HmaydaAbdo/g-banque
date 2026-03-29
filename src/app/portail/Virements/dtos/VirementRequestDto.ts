import {Type} from "../Type"

export interface VirementRequestDto {

  banqueBeneficiaireId:number
  dateReception:string
  beneficiaire:string
  montant:number
  emetteur:string
  type:Type

}
