import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import { Status } from "../../prefinancements/enums/Status";
import {Type} from "../../Virements/Type";


export interface ImpayeeSearchDto {
  banqueId?: number;
  libelle?: string;
  motif?: TransactionSource;
  montant?: number;
  status?: Status;
  type?:Type;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
