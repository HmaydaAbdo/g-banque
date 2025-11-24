import {PageResponse} from "../../../../global/types/PageResponse";
import {TransactionResponseDto} from "./TransactionResponseDto";

export interface AgreratedTransactionResponseDto {

  totalDebit:number,
  totalCredit:number,
  totalTvaDeductible:number,
  totalTvaCollectee:number,
  page:PageResponse<TransactionResponseDto>
}
