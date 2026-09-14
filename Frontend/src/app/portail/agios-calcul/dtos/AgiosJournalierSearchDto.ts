import { TypeAgios } from '../enums/TypeAgios';

export interface AgiosJournalierSearchDto {
  banqueId?: number;
  type?: TypeAgios;
  trimestre?: number;
  annee?: number;
  dateLimite?: string;
  marcheId?: number;
}


