import { TransactionSource } from '../../transactions/dtos/TransactionSource';
import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export enum SensMoyenPaiement {
  ENCAISSEMENT = 'ENCAISSEMENT',
  DECAISSEMENT = 'DECAISSEMENT'
}

export interface RecapMoyenPaiementItem {
  id: number;
  source: TransactionSource;
  banqueId: number | null;
  banqueNom: string | null;
  type: SensMoyenPaiement;
  statut: StatutMoyenPaiement;
  numero: string | null;
  dateEmission: string | null;
  dateOperation: string | null;
  dateValeur: string | null;
  dateEcheance: string | null;
  montant: number;
  libelle: string;
  observation: string;
  beneficiaire: string | null;
  emetteur: string | null;
}
