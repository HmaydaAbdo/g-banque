import { Type } from '../../Virements/Type';

export interface VirementInternationalRequestDto {
  banqueId: number;
  beneficiaire: string;
  emetteur: string;
  montant: number;
  dateEmission: string;
  type: Type;
}
