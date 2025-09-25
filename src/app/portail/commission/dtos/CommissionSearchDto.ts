import {TransactionSource} from "../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";

export interface CommissionSearchDto {
  banqueId?: number;
  libelle?: string;
  operation?: TransactionSource;
  status?: Status;
}
