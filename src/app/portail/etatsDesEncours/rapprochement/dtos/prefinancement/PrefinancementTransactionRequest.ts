export interface PrefinancementTransactionRequest {
  prefinancementId: number;
  banqueId: number;
  dateValeur: Date;
  dateOperation: Date;
  tva: number;
}
