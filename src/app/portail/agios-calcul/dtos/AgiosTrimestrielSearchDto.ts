import { TypeAgios } from '../enums/TypeAgios';
import { StatutTrimestre } from '../enums/StatutTrimestre';

export interface AgiosTrimestrielSearchDto {
  banqueId?: number;
  type?: TypeAgios;
  annee?: number;
  trimestre?: number;
  statut?: StatutTrimestre;
}
