import { Devise } from '../enums/Devise';

export interface BanqueRequest {
  nom: string;
  numeroDeCompte: string;
  devise: Devise;
  adresse: string;
  agence: string;
}
