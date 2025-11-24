import {Decimal} from "decimal.js";

export interface EncoursFactoringSearchDto {
  marcheName: string;
  marcheNumero: string;
  montant:Decimal
}
