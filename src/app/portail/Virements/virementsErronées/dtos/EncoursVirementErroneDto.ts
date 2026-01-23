import {PageResponse} from "../../../global/types/PageResponse";
import {VirementErroneResponseDto} from "./VirementErroneResponseDto";

export interface EncoursVirementErroneDto {
  totalDesEncours: number;
  page:PageResponse<VirementErroneResponseDto>
}
