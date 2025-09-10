import {TransactionSource} from "../transactions/TransactionSource";

export interface MiseAdispositionTransactionResponseDto {
  id: number;
  banqueName: string;
  miseADispositionNumber: string;
  libelle: string;
  observation: string;
  dateEmission: string;      // OffsetDateTime → string (ISO 8601)
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: number;
  tvaCollectee: number;
  debit: number;             // BigDecimal → number
  credit: number;
  transactionSource: TransactionSource;
}
