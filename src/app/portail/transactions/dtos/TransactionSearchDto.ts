import {TransactionSource} from "./TransactionSource";

export interface TransactionSearchDto {
  libelle?: string;
  observation?: string;
  banqueId?: number;
  transactionSources?: TransactionSource[];
  statutRapprochement?: 'EN_COURS' | 'CLOTURE';
  montant?: number;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
}
