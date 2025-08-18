import {Decimal} from "decimal.js";
import {BanqueDto} from "./BanqueDto";

export interface CreditMoyenTermeResponseDto {
  id: number;
  designation: string;
  nominal: Decimal;
  periodicite: number;
  duree: number;
  echeance: Decimal;
  taux: Decimal;
  dateDebut: string;
  dateCreation: string;
  dateModification: string;
  banque: BanqueDto;
  completed: boolean;
}
