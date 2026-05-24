export interface FactoringTransactionRequest {
  factoringId: number;
  banqueId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  libelle: string;
  observation: string;
  tva: number;
  rapprochementId: number;
}
