import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";

export interface DecaissementSearchDto {
  transactionSources?: TransactionSource[];
  observation:string;
  libelle:string;
  /**
   * Borne supérieure optionnelle sur la date de référence de chaque opération (colonne
   * "dateReference" côté vue SQL — déjà mappée par source : date d'émission, date d'échéance...).
   * Si non renseignée, le backend retombe sur la fin du mois en cours.
   */
  dateReferenceJusqua?: Date | null;
}
