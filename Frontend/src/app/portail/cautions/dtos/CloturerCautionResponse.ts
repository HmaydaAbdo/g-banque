import { CautionResponseDto } from './CautionResponseDto';
import { CommissionResponseDto } from '../../commission/dtos/commission-response.dto';

export interface CloturerCautionResponse {
  caution:    CautionResponseDto;
  commission: CommissionResponseDto;
}
