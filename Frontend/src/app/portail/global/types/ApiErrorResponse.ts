/**
 * Shape JSON retourné par le GlobalExceptionHandler du backend.
 *
 * Exemple :
 * {
 *   "status"    : 400,
 *   "error"     : "Bad Request",
 *   "message"   : "Toutes les commissions doivent appartenir à la même banque.",
 *   "traces"    : null,
 *   "timestamp" : "2025-09-17T10:30:00Z"
 * }
 */
export interface ApiErrorResponse {
  status:    number;
  error:     string;
  message:   string;
  traces:    string | null;
  timestamp: string;
}
