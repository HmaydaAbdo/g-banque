import {Decimal} from "decimal.js";

export interface LcnSearchDTO {
  numero?: string;
  dateEmission?: Date;
  dateEcheance?: Date;
  emetteur?:string
  beneficiaire?: string;
  nomBanque?: string;
  active?: boolean;
  montant?:Decimal
}
