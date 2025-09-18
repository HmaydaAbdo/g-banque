import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {VirementResponseDto} from "./VirementResponseDto";

export interface EncoursVirementResponseDto {

  totalDesEncours:Decimal
  page:PageResponse<VirementResponseDto>
}
