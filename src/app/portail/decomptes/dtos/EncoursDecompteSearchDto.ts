import {Decimal} from "decimal.js";

export interface EncoursDecompteSearchDto {
  marcheName?:string,
  marcheNumero?:string,
  montant?:Decimal
}
