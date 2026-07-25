import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export interface ChequeSearchDto {
  numero?: string;
  beneficiaire?: string;
  nomBanque?: string;
  statut?: StatutMoyenPaiement;
  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateEcheanceFrom?: Date | null;
  dateEcheanceTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
  montant?: number;
  emetteur?: string;
}
