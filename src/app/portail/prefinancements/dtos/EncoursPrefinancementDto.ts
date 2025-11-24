import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {PrefinancementResponseDTO} from "./PrefinancementResponseDTO";

export interface EncoursPrefinancementDto {

  totalDesEncours:number,
  page:PageResponse<PrefinancementResponseDTO>
}
