import {TransactionSource} from "./TransactionSource";

export interface TransactionSearchDto {
  libelle?: string;
  observation?: string;
  banqueId?: number;
  rapprochementId?: number;
  transactionSources?: TransactionSource[];
  statutRapprochement?: 'EN_COURS' | 'CLOTURE_PROVISOIRE' | 'CLOTURE_DEFINITIVE';
  montant?: number;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
