import { Type } from '../../Virements/Type';
import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export interface VirementInternationalSearchDto {
  banqueId?: number;
  beneficiaire?: string;
  emetteur?: string;
  montant?: number;
  libelle?: string;
  observation?: string;
  statut?: StatutMoyenPaiement;
  type?: Type;

  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
}
