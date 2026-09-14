import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {LcnResponseDTO} from "./LcnResponseDTO";

export interface EncoursLcnResponse {
  totalEncours:number
  page:PageResponse<LcnResponseDTO>
}
