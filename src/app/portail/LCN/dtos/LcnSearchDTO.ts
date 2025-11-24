import {Decimal} from "decimal.js";

export interface LcnSearchDTO {
  numero?: string;
  beneficiaire?: string;
  nomBanque?: string;
  active?: boolean;
  montant?:Decimal
}
