export interface PrefinancementTransactionRequest {
  prefinancementId: number;
  banqueId: number;
  dateValeur: string;
  dateOperation: string;
  dateEmission:string
  libelle:string
  observation:string
  tauxTva: number | null;
  montantTva: number | null;
  rapprochementId: number;
}
