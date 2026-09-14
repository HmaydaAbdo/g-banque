import { FraisBancaireResponseDto } from './FraisBancaireResponseDto';
import { PageResponse } from '../../global/types/PageResponse';

export interface EncoursFraisBancaireDto {
  totalDesEncours: number;
  page: PageResponse<FraisBancaireResponseDto>;
}
