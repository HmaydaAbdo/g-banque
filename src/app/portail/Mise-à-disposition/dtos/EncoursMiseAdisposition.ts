import {Decimal} from "decimal.js";
import {PageResponse} from "../../global/types/PageResponse";
import {MiseADispositionResponseDto} from "./MiseADispositionResponseDto";

export interface EncoursMiseAdisposition {
  totalDesEncours:Decimal
  page:PageResponse<MiseADispositionResponseDto>
}
