import { Status } from "../../prefinancements/enums/Status";
import {TransactionSource} from "../../transactions/dtos/TransactionSource";

export interface OperationErroneSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: number;
  status?: Status;
  motif?: TransactionSource;
}
