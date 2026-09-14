import {PageResponse} from "../../global/types/PageResponse";
import {VirementResponseDto} from "./VirementResponseDto";

export interface EncoursVirementResponseDto {

  totalDesEncours:number
  page:PageResponse<VirementResponseDto>
}
