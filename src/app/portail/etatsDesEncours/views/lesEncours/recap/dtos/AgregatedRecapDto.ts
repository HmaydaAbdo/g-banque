import {PageResponse} from "../../../../../global/types/PageResponse";
import {RecaputilatifItem} from "./RecaputilatifItem";

export interface AgregatedRecapDto {

  totalCredit:number,
  totalDebit:number,
  page:PageResponse<RecaputilatifItem>
}
