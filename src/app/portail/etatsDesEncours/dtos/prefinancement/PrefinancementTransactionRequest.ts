export interface PrefinancementTransactionRequest {
  prefinancementId: number;
  banqueId: number;
  dateValeur: Date;
  dateOperation: Date;
  dateEmission:Date
  libelle:string
  observation:string
  tva: number;
}
