import {Decimal} from "decimal.js";

export interface SousLigneRequestDto {

  id?:number,
  plafond:Decimal,
  natureLigneFonctionnelleId:number
}
