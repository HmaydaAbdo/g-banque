export interface DroitConstateTransactionRequest {
  droitConstateId: number;
  banqueId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  tva: number;
  libelle: string;
  observation: string;
}
