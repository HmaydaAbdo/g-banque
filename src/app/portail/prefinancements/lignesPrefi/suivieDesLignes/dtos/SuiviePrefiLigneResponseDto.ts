import {Decimal} from "decimal.js";
import {Status} from "../../../enums/Status";
import {Type} from "../../../enums/Type";

export interface SuiviePrefiLigneResponseDto {
  id: number;
  dateEmission: Date;
  dateOperation: Date;
  dateValeur: Date;
  libelle: string;
  debit: Decimal;
  credit: Decimal;
  observation: string;
  status: Status;
  type: Type;
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
}
