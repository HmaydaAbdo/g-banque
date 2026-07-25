import {Decimal} from "decimal.js";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

export interface LcnSearchDTO {
  numero?: string;
  /** @deprecated Utilisé uniquement par les écrans d'encours par banque (hors périmètre) — la liste principale utilise dateEmissionFrom/To. */
  dateEmission?: Date;
  /** @deprecated Utilisé uniquement par les écrans d'encours par banque (hors périmètre) — la liste principale utilise dateEcheanceFrom/To. */
  dateEcheance?: Date;
  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateEcheanceFrom?: Date | null;
  dateEcheanceTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
  emetteur?:string
  beneficiaire?: string;
  nomBanque?: string;
  statut?: StatutMoyenPaiement;
  montant?:Decimal
}
