import { Decimal } from 'decimal.js';

export interface MarcheRequestDTO {
  numero: string;
  nom: string;
  montant: Decimal;
  maitreDouvrage: string;
  dateDebutDesTravaux?: string;
  dateDAchievementDesTravaux?: string;
  banqueId: number;
}
