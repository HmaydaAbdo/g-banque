import {Devise} from "../enums/Devise";


export interface BanqueRequest {
  nom: string;
  numeroDeCompte: string;
  devise: Devise;
  address: string;
  agence: string;
}
