export interface DroitConstateTransactionRequest {
  droitConstateId: number;
  banqueId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  tauxTva: number | null;
  montantTva: number | null;
  libelle: string;
  observation: string;
  rapprochementId: number;
}
