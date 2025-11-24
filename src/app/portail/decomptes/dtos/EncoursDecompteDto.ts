import {PageResponse} from "../../global/types/PageResponse";
import {DecompteResponseDto} from "./DecompteResponseDto";

export interface EncoursDecompteDto {
  totalDesEncours:number
  page:PageResponse<DecompteResponseDto>
}
