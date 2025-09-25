import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {CommissionResponseDto} from "./commission-response.dto";

export interface EncoursCommissionDto {

  totalDesEncours:Decimal
  page:PageResponse<CommissionResponseDto>
}
