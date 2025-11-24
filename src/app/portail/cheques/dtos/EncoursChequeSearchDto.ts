import {Decimal} from "decimal.js";

export interface EncoursChequeSearchDto {
  beneficiaire?:string
  numero?:string
  montant?:Decimal
}
