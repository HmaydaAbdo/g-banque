import { FraisBancaireResponseDto } from './FraisBancaireResponseDto';
import { PageResponse } from '../../global/types/PageResponse';
import {Decimal} from "decimal.js";

export interface EncoursFraisBancaireDto {
  totalDesEncours: Decimal;
  page: PageResponse<FraisBancaireResponseDto>;
}
