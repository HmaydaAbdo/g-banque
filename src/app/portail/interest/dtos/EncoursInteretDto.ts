import {PageResponse} from "../../global/types/PageResponse";
import {InteretResponseDto} from "./InteretResponseDto";

export interface EncoursInteretDto {
  totalDesEncours: number;
  page: PageResponse<InteretResponseDto>;
}
