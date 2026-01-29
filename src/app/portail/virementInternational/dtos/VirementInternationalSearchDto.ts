import { Type } from '../../Virements/Type';

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
  active?: boolean;
  type?: Type;
}
