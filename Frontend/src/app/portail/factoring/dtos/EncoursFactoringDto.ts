import { FactoringResponseDto } from './FactoringResponseDto';
import {PageResponse} from "../../global/types/PageResponse";
import {Decimal} from "decimal.js";


export interface EncoursFactoringDto {
  totalDesEncours: number;
  page: PageResponse<FactoringResponseDto>;
}
