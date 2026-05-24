export interface DecompteTransactionRequest {

  decompteId: number;
  banqueId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  tva: number;
  libelle: string;
  observation: string;
  rapprochementId: number;

}
