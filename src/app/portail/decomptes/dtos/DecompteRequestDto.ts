import {Decimal} from "decimal.js";

export interface DecompteRequestDto {

  marcheId: number;
  dateEmission: Date;
  banqueEncaissementId?: number;
  montant: Decimal;
  libelle:string

}
