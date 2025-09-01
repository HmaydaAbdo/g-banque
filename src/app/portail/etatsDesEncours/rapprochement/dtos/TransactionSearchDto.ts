/**
 * DTO for searching transactions.
 * Matches the backend's search criteria.
 * All fields are optional to support flexible searching.
 */
export interface TransactionSearchDto {
  libelle?: string;
  banqueId?: number;
  transactionSource?: string;
}
