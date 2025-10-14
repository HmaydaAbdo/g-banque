import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {InteretResponseDto} from "./InteretResponseDto";

export interface EncoursInteretDto {
  totalDesEncours: Decimal;
  page: PageResponse<InteretResponseDto>;
}
