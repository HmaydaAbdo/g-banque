import {Decimal} from "decimal.js";

export interface EncoursVirementsSearchDto {
  beneficiaire?:string
  numero?:string
  montant?:Decimal
}
