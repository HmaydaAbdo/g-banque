import {Decimal} from "decimal.js";

export interface DecompteRequestDto {

  marcheId: number;
  dateEmission: string;
  banqueEncaissementId?: number;
  montant: Decimal;
  libelle:string

}
