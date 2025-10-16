import { FactoringResponseDto } from './FactoringResponseDto';
import {PageResponse} from "../../global/types/PageResponse";
import {Decimal} from "decimal.js";


export interface EncoursFactoringDto {
  totalDesEncours: Decimal;
  page: PageResponse<FactoringResponseDto>;
}
