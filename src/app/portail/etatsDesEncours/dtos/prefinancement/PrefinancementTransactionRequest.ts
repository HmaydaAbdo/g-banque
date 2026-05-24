export interface PrefinancementTransactionRequest {
  prefinancementId: number;
  banqueId: number;
  dateValeur: string;
  dateOperation: string;
  dateEmission:string
  libelle:string
  observation:string
  tva: number;
  rapprochementId: number;
}
