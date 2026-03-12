import {Type} from "../../prefinancements/enums/Type";
import {Decimal} from "decimal.js";

export interface DroitConstateRequestDto {
  marcheId: number;
  dateEmission: Date;
  montant: number;
  type: Type;
}


