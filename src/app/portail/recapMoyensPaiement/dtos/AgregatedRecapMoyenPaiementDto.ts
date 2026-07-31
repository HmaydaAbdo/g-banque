import { PageResponse } from '../../global/types/PageResponse';
import { RecapMoyenPaiementItem } from './RecapMoyenPaiementItem';

export interface AgregatedRecapMoyenPaiementDto {
  total: number;
  page: PageResponse<RecapMoyenPaiementItem>;
}
