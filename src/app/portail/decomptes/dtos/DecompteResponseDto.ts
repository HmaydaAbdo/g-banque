import {Decimal} from "decimal.js";
import {Type} from "../../prefinancements/enums/Type";
import {Status} from "../../prefinancements/enums/Status";

export interface DecompteResponseDto {
  id: number;
  marcheId: number;
  marcheName: string;
  marcheNumero: string;
  banqueId: number;
  banqueName: string;
  dateEmission: Date;
  montant: Decimal;
  observation: string;
  dateOperation: string;
  dateValeur: string;
  status: Status;
}
