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
  banqueDePrelevementId: number;
  banqueDePrelevementNom: string;
  statutRapprochement: 'EN_COURS' | 'CLOTURE_PROVISOIRE' | 'CLOTURE_DEFINITIVE';
  referent?: number | null;
  createdAt: Date;
  updatedAt: Date;
}
