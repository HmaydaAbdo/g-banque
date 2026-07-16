import { TypeChargeFixe } from './TypeChargeFixe';
import { Motif } from '../../../impotEtTaxes/dtos/Motif';

export interface ChargeFixeConfigRequest {
  banqueId: number;
  libelle: string;
  montant: number;
  type: TypeChargeFixe;
  motif?: Motif | null;
  jourEmission: number;
  appendPeriode: boolean;
  moisPrecedent: boolean;
}
