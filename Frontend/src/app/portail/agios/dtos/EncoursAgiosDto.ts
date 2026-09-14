import {PageResponse} from "../../global/types/PageResponse";
import {AgiosResponseDto} from "./AgiosResponseDto";

export interface EncoursAgiosDto {
  totalDesEncours: number;
  page: PageResponse<AgiosResponseDto>;
}
