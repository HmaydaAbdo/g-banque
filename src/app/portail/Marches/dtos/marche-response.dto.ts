import {Decimal} from "decimal.js";

export interface MarcheResponseDTO {
  id: number;
  numero: string;
  nom: string;
  montant: Decimal;
  maitreDouvrage: string;
  delai: Date;
  createdAt: Date;
  modifiedAt: Date;
}
