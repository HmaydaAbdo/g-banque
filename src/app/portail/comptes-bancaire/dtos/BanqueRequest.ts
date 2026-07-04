import { Devise } from '../enums/Devise';

export interface BanqueRequest {
  nom: string;
  numeroDeCompte: string;
  devise: Devise;
  adresse: string;
  agence: string;
  /**
   * Solde de départ, obligatoire à la création — devient le soldeDebut du premier
   * rapprochement ouvert automatiquement. Ignoré par le backend en modification
   * (non stocké sur la banque, non modifiable après création).
   */
  soldeInitial: number;
}
