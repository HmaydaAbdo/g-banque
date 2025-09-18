import {TransactionSource} from "../transactions/TransactionSource";
import {Decimal} from "decimal.js";

export interface MiseAdispositionTransactionResponseDto {
  id: number;
  banqueName: string;
  miseADispositionNumber: string;
  libelle: string;
  observation: string;
  dateEmission: string;      // OffsetDateTime → string (ISO 8601)
  dateValeur: string;
  dateOperations: string;
  tvaDeductible: Decimal;
  tvaCollectee: Decimal;
  debit: number;             // BigDecimal → number
  credit: number;
  transactionSource: TransactionSource;
}
