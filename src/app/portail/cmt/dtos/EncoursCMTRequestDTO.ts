import {Decimal} from "decimal.js";


export interface EncoursCMTRequestDTO {
  banqueId: number;
  designation:string
  montant:Decimal
  page?: number;
  size?: number;
  sort?: string;
}
