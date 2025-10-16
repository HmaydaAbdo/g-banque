import { Decimal } from 'decimal.js';

export interface MarcheRequestDTO {
  numero: string;
  nom: string;
  montant: Decimal;
  maitreDouvrage: string;
  dateDebutDesTravaux?: Date;
  dateDAchievementDesTravaux?: Date;
  banqueId: number;
}
