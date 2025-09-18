import {PageResponse} from "../../global/types/PageResponse";
import {DroitConstateResponseDto} from "./DroitConstateResponseDto";
import {Decimal} from "decimal.js";

export interface EncoursDroitConstateDto {
  totalDesEncours: Decimal;
  page: PageResponse<DroitConstateResponseDto>;
}
