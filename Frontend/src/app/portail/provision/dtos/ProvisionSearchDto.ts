import { Status } from "../../prefinancements/enums/Status";
import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../transactions/dtos/SensOperation";

export interface ProvisionSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: number;
  status?: Status;
  motif?: TransactionSource;
  sens?: SensOperation;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
