import { Devise } from '../enums/Devise';

export interface BanqueResponse {
  id: number;
  logoBase64: string;
  nom: string;
  numeroDeCompte: string;
  devise: Devise;
  adresse: string;
  agence: string;
  soldeActuel: number;
  dateCreation: string;
  dateModification: string;
}
