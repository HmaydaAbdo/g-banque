import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";
import {Decimal} from "decimal.js";

export interface CommissionSearchDto {
  banqueId?: number;
  libelle?: string;
  operation?: TransactionSource;
  montant?: Decimal;
  status?: Status;
}
