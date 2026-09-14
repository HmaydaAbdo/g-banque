import {Decimal} from "decimal.js";

export interface SousLigneResponseDto {

  id:number,
  plafond:Decimal,
  natureLigneFonctionnelleId:number,
  natureLigneFonctionnelleLabel:string,
  actif:boolean,
}
