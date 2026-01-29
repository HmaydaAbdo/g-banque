import {VirementInternationalResponseDto} from "./VirementInternationalResponseDto";
import {PageResponse} from "../../global/types/PageResponse";

export interface EncoursVirementInternationalResponseDto {
  totalDesEncours: number;
  page: PageResponse<VirementInternationalResponseDto>;
}
