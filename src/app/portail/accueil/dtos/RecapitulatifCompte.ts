import {Devise} from "../../comptes-bancaire/enums/Devise";

export interface RecapitulatifCompte {
  banqueId:   number;
  nom:        string;
  logoBase64: string;
  soldeActuel:   number;
  soldeDebut:    number;
  plafondFC?:    number;
  devise:        Devise;
  lcnsEchusCount:      number;
  virementsEchusCount: number;
}
