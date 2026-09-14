import {Decimal} from "decimal.js";
import {Type} from "../../prefinancements/enums/Type";
import {Status} from "../../prefinancements/enums/Status";

export interface DroitConstateResponseDto {
  id: number;
  marcheId: number;
  marcheName: string;
  marcheNumero: string;
  banqueId: number;
  banqueName: string;
  dateEmission: string;
  montant: number;
  observation: string;
  type: Type;
  dateOperation: string;
  dateValeur: string;
  status: Status;
  libelle: string;
}

