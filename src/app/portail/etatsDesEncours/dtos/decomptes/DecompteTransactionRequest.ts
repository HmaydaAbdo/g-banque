export interface DecompteTransactionRequest {

  decompteId: number;
  banqueId: number;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  tva: number;
  libelle: string;
  observation: string;

}
