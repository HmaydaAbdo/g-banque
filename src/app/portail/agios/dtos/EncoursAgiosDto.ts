import {PageResponse} from "../../global/types/PageResponse";
import {AgiosResponseDto} from "./AgiosResponseDto";
import {Decimal} from "decimal.js";

export interface EncoursAgiosDto {
  totalDesEncours: Decimal;
  page: PageResponse<AgiosResponseDto>;
}
