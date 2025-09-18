import {Decimal} from "decimal.js";

export interface DecompteRequestDto {

  marcheId: number;
  banqueId:number;
  dateEmission: Date;
  montant: Decimal;

}
