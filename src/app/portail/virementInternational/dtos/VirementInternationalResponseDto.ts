import { Type } from '../../Virements/Type';

export interface VirementInternationalResponseDto {
  id: number;
  banqueId: number;
  banqueNom: string;
  beneficiaire: string;
  emetteur: string;
  montant: number;
  dateEmission: Date;
  dateOperation: Date;
  dateValeur: Date;
  libelle: string;
  observation: string;
  active: boolean;
  type: Type;
}
