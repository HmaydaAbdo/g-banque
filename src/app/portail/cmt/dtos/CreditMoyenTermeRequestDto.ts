import {Decimal} from "decimal.js";

export interface CreditMoyenTermeRequestDto {
  banqueId: number;
  designation: string;
  nominal: Decimal;
  periodicite: number;
  duree: number;
  echeance: Decimal;
  taux: Decimal;
  dateDebut: string;
}
