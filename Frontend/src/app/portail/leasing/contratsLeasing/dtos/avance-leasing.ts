import { StatutEcheanceLeasing } from './StatutEcheanceLeasing';

/** Moyen de paiement pouvant régler l'avance d'un contrat de leasing. */
export type TypeMoyenPaiementAvance = 'CHEQUE' | 'LCN' | 'VIREMENT';

export type StatutMoyenPaiementAvance = 'ENCOURS' | 'ANNULE' | 'REGLE';

/** Chèque, LCN ou virement qui règle (ou peut régler) l'avance. */
export interface MoyenPaiementAvance {
  type: TypeMoyenPaiementAvance;
  id: number;
  numero: string | null;
  montant: number;
  statut: StatutMoyenPaiementAvance;
  dateEmission: string | null;
  dateOperation: string | null;
  dateValeur: string | null;
  beneficiaire: string | null;
  nomBanque: string | null;
}

/** Avance du contrat (échéance n°1) et son moyen de paiement ; `moyenPaiement` est null tant qu'elle n'est pas rattachée. */
export interface AvanceLeasingResponse {
  echeanceId: number;
  contratLeasingId: number;
  montantTTC: number;
  dateEcheance: string;
  statut: StatutEcheanceLeasing;
  moyenPaiement: MoyenPaiementAvance | null;
}

export const LIBELLE_TYPE_MOYEN_PAIEMENT: Record<TypeMoyenPaiementAvance, string> = {
  CHEQUE: 'Chèque',
  LCN: 'LCN',
  VIREMENT: 'Virement'
};
