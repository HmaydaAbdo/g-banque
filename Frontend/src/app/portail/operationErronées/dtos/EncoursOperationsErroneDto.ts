import {OperationErroneResponseDto} from "./OperationErroneResponseDto";
import {PageResponse} from "../../global/types/PageResponse";

export interface EncoursOperationsErroneDto {
  totalDesEncours: number;
  page:PageResponse<OperationErroneResponseDto>
}
