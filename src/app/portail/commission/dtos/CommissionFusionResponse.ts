import { CommissionResponseDto } from './commission-response.dto';
import { ImpotEtTaxeResponseDto } from '../../impotEtTaxes/dtos/ImpotEtTaxeResponseDto';

export interface CommissionFusionResponse {
  commission: CommissionResponseDto;
  /** null si genererTva = false */
  tva: ImpotEtTaxeResponseDto | null;
}
