import {Decimal} from "decimal.js";

export interface EncoursVirementsSearchDto {
  beneficiaire?:string
  dateEmission?:Date
  montant?:Decimal
  emetteur?:string
}
