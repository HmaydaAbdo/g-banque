import {Decimal} from "decimal.js";

export interface DecompteRequestDto {

  marcheId: number;
  dateEmission: Date;
  montant: Decimal;
  libelle:string

}
