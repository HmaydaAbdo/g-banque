import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";

/** Indicateurs d'une source — réponse de GET /recapitulatif/banque/{id}/par-source. */
export interface RecapSourceDto {
  source: TransactionSource;
  operationsCount: number;
  totalDebit: number;
  totalCredit: number;
  /** Encours dont la date de référence est atteinte à ce jour. */
  echuesCount: number;
}
