import {PageResponse} from "../../global/types/PageResponse";
import {CommissionResponseDto} from "./commission-response.dto";

export interface EncoursCommissionDto {

  totalDesEncours:number
  page:PageResponse<CommissionResponseDto>
}
