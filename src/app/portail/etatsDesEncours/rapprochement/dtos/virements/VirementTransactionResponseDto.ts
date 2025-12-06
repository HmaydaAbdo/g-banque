import {TransactionSource} from "../../../../transactions/dtos/TransactionSource";
import {Decimal} from "decimal.js";

export interface VirementTransactionResponseDto {
  id: number;
  banqueName: string;
  virementNumber: string;
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
