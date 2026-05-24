import {TransactionSource} from "./TransactionSource";

export interface TransactionSearchDto {
  libelle?: string;
  observation?: string;
  banqueId?: number;
  transactionSources?: TransactionSource[];
  statutRapprochement?: 'EN_COURS' | 'CLOTURE_PROVISOIRE' | 'CLOTURE_DEFINITIVE';
  montant?: number;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
}
