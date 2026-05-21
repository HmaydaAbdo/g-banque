import {Decimal} from "decimal.js";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

export interface LcnSearchDTO {
  numero?: string;
  dateEmission?: Date;
  dateEcheance?: Date;
  emetteur?:string
  beneficiaire?: string;
  nomBanque?: string;
  statut?: StatutMoyenPaiement;
  montant?:Decimal
}
