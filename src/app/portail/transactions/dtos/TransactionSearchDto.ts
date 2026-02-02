import {TransactionSource} from "./TransactionSource";

export interface TransactionSearchDto {
  libelle?: string;
  observation?: string;
  banqueId?: number;
  transactionSources?: TransactionSource[];
  isConfirmed?: boolean;
  montant?: number;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
}
