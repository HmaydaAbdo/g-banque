import {Decimal} from "decimal.js";

export interface EncoursLeasingRequestDTO {
  banqueId: number;
  organismeLeasingId?: number; // Le '?' rend le champ optionnel
  numeroContrat:string
  montant:Decimal
  page?: number;
  size?: number;
  sort?: string;
}
