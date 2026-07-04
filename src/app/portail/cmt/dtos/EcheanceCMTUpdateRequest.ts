export interface EcheanceCMTUpdateRequest {
  dateEcheance: string;       // LocalDate — format ISO 'YYYY-MM-DD'
  montant: number;
  dateOperation?: string | null;
  dateValeur?: string | null;
  observation?: string | null;
  libelle?: string | null;
}
