export interface FactoringTransactionRequest {
  factoringId: number;
  banqueId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  libelle: string;
  observation: string;
  tauxTva: number | null;
  montantTva: number | null;
  rapprochementId: number;
}
