import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import { Status } from "../../prefinancements/enums/Status";


export interface ImpayeeSearchDto {
  banqueId?: number;
  libelle?: string;
  motif?: TransactionSource;
  montant?: number;
  status?: Status;
}
