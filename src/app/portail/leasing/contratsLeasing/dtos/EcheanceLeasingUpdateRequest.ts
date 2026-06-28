export interface EcheanceLeasingUpdateRequest {
  dateEcheance: string;       // LocalDate — format ISO 'YYYY-MM-DD'
  montantTTC: number;
  dateOperation?: string | null;
  dateValeur?: string | null;
  observation?: string | null;
  libelle?: string | null;
}
