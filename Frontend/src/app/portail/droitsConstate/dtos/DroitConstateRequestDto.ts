import {Type} from "../../prefinancements/enums/Type";
import {Decimal} from "decimal.js";

export interface DroitConstateRequestDto {
  marcheId: number;
  /**
   * Identifiant de la banque liée à ce droit constaté.
   * Optionnel : si absent, le backend utilisera la banque du marché par défaut.
   * - DEBLOCAGE  → banque d'encaissement (reçoit les fonds du Trésor)
   * - REMBOURSEMENT → banque de décaissement (rembourse le crédit)
   */
  banqueId: number;
  dateEmission: string;
  montant: number;
  type: Type;
}


