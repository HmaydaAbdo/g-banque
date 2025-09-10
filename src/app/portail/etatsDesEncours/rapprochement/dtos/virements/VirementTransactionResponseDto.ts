import {TransactionSource} from "../transactions/TransactionSource";

export interface VirementTransactionResponseDto {
  id: number;
  banqueName: string;
  virementNumber: string;
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
