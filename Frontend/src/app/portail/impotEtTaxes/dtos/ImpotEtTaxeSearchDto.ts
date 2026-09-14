import { Status } from "../../prefinancements/enums/Status";
import { Decimal } from "decimal.js";
import {Motif} from "./Motif";

export interface ImpotEtTaxeSearchDto {
  banqueId?: number;
  libelle?: string;
  motif?: Motif;
  montant?: Decimal;
  status?: Status;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
