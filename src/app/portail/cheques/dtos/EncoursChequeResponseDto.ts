import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {ChequeResponseDto} from "./ChequeResponseDto";

export interface EncoursChequeResponseDto {

  totalDesEncours:Decimal
  page:PageResponse<ChequeResponseDto>
}
