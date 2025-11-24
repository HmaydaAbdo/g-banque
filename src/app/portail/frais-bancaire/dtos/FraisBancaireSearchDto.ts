import {Status} from "../../prefinancements/enums/Status";
import {Decimal} from "decimal.js";

export interface FraisBancaireSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: Decimal;
  status?: Status;
}
