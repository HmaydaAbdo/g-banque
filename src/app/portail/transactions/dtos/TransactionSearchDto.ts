import {TransactionSource} from "./TransactionSource";

/**
 * DTO for searching dtos.
 * Matches the backend's search criteria.
 * All fields are optional to support flexible searching.
 */
export interface TransactionSearchDto {
  libelle?: string;
  observation?: string;
  banqueId?: number;
  transactionSource?: TransactionSource[];
}
