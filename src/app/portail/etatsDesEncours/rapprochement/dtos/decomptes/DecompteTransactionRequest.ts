export interface DecompteTransactionRequest {

  decompteId: number;
  banqueId: number;
  dateValeur: Date;
  dateOperation: Date;
  tva: number;

}
