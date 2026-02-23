import { TypeAgios } from '../enums/TypeAgios';

export interface AgiosJournalierResponseDto {
  id: number;
  typeAgios: TypeAgios;
  dateValeur: string;
  banqueId: number;
  banqueNom: string;
  soldeJournalier: number;
  baseCalcul: number;
  tauxApplique: number;
  agiosHT: number;
  tva: number;
  agiosTTC: number;
  dateCalcul: string;
  nbRecalculs: number;
}
