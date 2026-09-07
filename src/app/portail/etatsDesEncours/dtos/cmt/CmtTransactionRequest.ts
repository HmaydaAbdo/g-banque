export interface CmtTransactionRequest {

  creditMoyenTermeId: number;
  echeanceId: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  tauxTva: number | null;
  montantTva: number | null;
  rapprochementId: number;
}
