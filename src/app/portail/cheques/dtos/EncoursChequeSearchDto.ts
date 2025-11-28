import {Decimal} from "decimal.js";

export interface EncoursChequeSearchDto {
  beneficiaire?:string
  numero?:string
  dateEmission?:Date
  emetteur?:string
  montant?:Decimal
}
