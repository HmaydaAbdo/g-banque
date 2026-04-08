import {Decimal} from "decimal.js";
import {TransactionSource} from "./TransactionSource";

/**
 * Interface for the TransactionResponseDto.
 * Represents a transaction from the backend API.
 */
export interface TransactionResponseDto {
  id: number;
  dateEmission: string;
  dateValeur: string;
  dateOperations: string;
  observation: string;
  tvaDeductible: number ;
  tvaCollectee: number;
  libelle: string;
  transactionSource: TransactionSource;
  debit: number;
  credit: number;
  traces: string;
  banqueDePrelevementId: number;
  banqueDePrelevementNom: string;
  statutRapprochement: 'EN_COURS' | 'CLOTURE';
  referent: number;
  createdAt: Date;
  updatedAt: Date;
}


