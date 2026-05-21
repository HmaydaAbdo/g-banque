import { Type } from '../../Virements/Type';
import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export interface VirementInternationalSearchDto {
  banqueId?: number;
  beneficiaire?: string;
  emetteur?: string;
  montant?: number;
  dateEmission?: string;
  dateOperation?: string;
  dateValeur?: string;
  libelle?: string;
  observation?: string;
  statut?: StatutMoyenPaiement;
  type?: Type;
}
