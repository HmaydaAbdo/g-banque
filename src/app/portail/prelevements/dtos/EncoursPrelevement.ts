import { PrelevementResponseDto } from './PrelevementResponseDto';
import {PageResponse} from "../../global/types/PageResponse";

export interface EncoursPrelevement {
  totalDesEncours: number;
  page: PageResponse<PrelevementResponseDto>;
}
