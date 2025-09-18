import { Decimal } from 'decimal.js';

export interface MarcheResponseDTO {
  id: number;
  numero: string;
  nom: string;
  montant: Decimal;
  maitreDouvrage: string;
  dateDebutDesTravaux: Date;
  dateDAchievementDesTravaux: Date;
  createdAt: Date;
  modifiedAt: Date;
}
