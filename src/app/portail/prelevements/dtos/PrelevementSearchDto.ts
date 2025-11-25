import {Decimal} from "decimal.js";
import {Status} from "../../prefinancements/enums/Status";

export interface PrelevementSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: Decimal;
  status?: Status;
}
