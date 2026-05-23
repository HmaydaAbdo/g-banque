import {Devise} from "../../comptes-bancaire/enums/Devise";

export interface RecapitulatifCompte {
  bankId:   number;
  name:     string;
  logoBase64: string;
  soldeActuelle: number;
  soldeDebut:    number;
  plafondFC?:    number;
  devise:        Devise;
  lcnsEchusCount:      number;
  virementsEchusCount: number;
}
