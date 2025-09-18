import {Decimal} from "decimal.js";
import {PageResponse} from "../../../../global/types/PageResponse";
import {TransactionResponseDto} from "./TransactionResponseDto";

export interface AgreratedTransactionResponseDto {

  totalDebit:Decimal,
  totalCredit:Decimal,
  totalTvaDeductible:Decimal,
  totalTvaCollectee:Decimal,
  page:PageResponse<TransactionResponseDto>
}
