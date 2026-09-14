import {Decimal} from "decimal.js";
import {Type} from "../../prefinancements/enums/Type";

export interface FactoringRequestDto {
  marcheId: number;
  dateEmission: string;
  montant: Decimal;
  type: Type;
}
