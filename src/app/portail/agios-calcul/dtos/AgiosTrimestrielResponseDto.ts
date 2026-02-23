import { TypeAgios } from '../enums/TypeAgios';
import { StatutTrimestre } from '../enums/StatutTrimestre';
import { AgiosJournalierResponseDto } from './AgiosJournalierResponseDto';

export interface AgiosTrimestrielResponseDto {
  id: number;
  typeAgios: TypeAgios;
  trimestre: number;
  annee: number;
  dateDebut: string;
  dateFin: string;
  banqueId: number;
  banqueNom: string;
  totalAgiosHT: number;
  totalTVA: number;
  totalAgiosTTC: number;
  nbJoursCalcules: number;
  nbJoursTotal: number;
  statut: StatutTrimestre;
  dateDernierCalcul: string;
  dateCloture: string | null;
  agiosDefinitifId: number | null;
  detailJournalier?: AgiosJournalierResponseDto[];
}
