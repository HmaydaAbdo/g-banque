import { TypeChargeFixe } from './TypeChargeFixe';
import { Motif } from '../../../impotEtTaxes/dtos/Motif';

export interface ChargeFixeConfigResponse {
  id: number;
  banqueId: number;
  banqueNom: string;
  libelle: string;
  montant: number;
  type: TypeChargeFixe;
  motif?: Motif | null;
  jourEmission: number;
  appendPeriode: boolean;
  moisPrecedent: boolean;
}
