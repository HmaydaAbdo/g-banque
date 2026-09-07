import {TransactionSource} from "../../../transactions/dtos/TransactionSource";

export interface ChequeTransactionRequest {
  idCheque: number;
  transactionSource: TransactionSource;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  /**
   * Saisie de la TVA : `tauxTva` et `montantTva` sont mutuellement exclusifs — exactement l'un des
   * deux est renseigné. La conversion taux → montant reste serveur (`TvaResolver`) ; le frontend
   * ne transmet jamais un montant dérivé d'un taux.
   */
  tauxTva: number | null;
  montantTva: number | null;
  libelle: string;
  rapprochementId: number;
}
