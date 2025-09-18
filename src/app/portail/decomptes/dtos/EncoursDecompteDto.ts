import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {DecompteResponseDto} from "./DecompteResponseDto";

export interface EncoursDecompteDto {
  totalDesEncours:Decimal
  page:PageResponse<DecompteResponseDto>
}
