import {Type} from "../../prefinancements/enums/Type";
import {Decimal} from "decimal.js";

export interface DroitConstateRequestDto {
  marcheId: number;
  dateEmission: string;
  montant: number;
  type: Type;
}


