import {Decimal} from "decimal.js";
import {PageResponse} from "../../../../../global/types/PageResponse";
import {RecaputilatifItem} from "./RecaputilatifItem";

export interface AgregatedRecapDto {

  totalCredit:Decimal,
  totalDebit:Decimal,
  page:PageResponse<RecaputilatifItem>
}
