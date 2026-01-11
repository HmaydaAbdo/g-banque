import { Decimal } from 'decimal.js';

export interface MarcheResponseDTO {
  id: number;
  numero: string;
  nom: string;
  montant: number;
  maitreDouvrage: string;
  dateDebutDesTravaux?: Date;
  dateDAchievementDesTravaux?: Date;
  banqueNom:string;
  banqueId:number;
  nanti: boolean;
  createdAt: Date;
  modifiedAt: Date;
}
