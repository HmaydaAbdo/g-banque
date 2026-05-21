import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export interface ChequeSearchDto {
  numero?: string;
  beneficiaire?: string;
  nomBanque?: string;
  statut?: StatutMoyenPaiement;
  dateEmission?: Date;
  montant?: number;
  emetteur?: string;
}
