import {BanqueDto} from "./BanqueDto";

export interface CreditMoyenTermeResponseDto {
  id: number;
  designation: string;
  nominal: number;
  periodicite: number;
  duree: number;
  echeance: number;
  taux: number;
  dateDebut: string;
  dateCreation: string;
  dateModification: string;
  banque: BanqueDto;
  completed: boolean;
}
