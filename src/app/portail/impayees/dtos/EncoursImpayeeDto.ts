import { PageResponse } from '../../global/types/PageResponse';
import { ImpayeeResponseDto } from './ImpayeeResponseDto';

export interface EncoursImpayeeDto {
  totalDesEncours: number;
  page: PageResponse<ImpayeeResponseDto>;
}
