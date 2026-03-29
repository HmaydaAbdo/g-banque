import { Decimal } from 'decimal.js';

export interface MarcheResponseDTO {
  id: number;
  numero: string;
  nom: string;
  montant: number;
  maitreDouvrage: string;
  dateDebutDesTravaux?: string;
  dateDAchievementDesTravaux?: string;
  banqueNom:string;
  banqueId:number;
  nanti: boolean;
  createdAt: Date;
  modifiedAt: Date;
}
