import {PageResponse} from "../../global/types/PageResponse";
import {DroitConstateResponseDto} from "./DroitConstateResponseDto";
import {Decimal} from "decimal.js";

export interface EncoursDroitConstateDto {
  totalDesEncours: number;
  page: PageResponse<DroitConstateResponseDto>;
}
