import {PageResponse} from "../../global/types/PageResponse";
import {MiseADispositionResponseDto} from "./MiseADispositionResponseDto";

export interface EncoursMiseAdisposition {
  totalDesEncours:number
  page:PageResponse<MiseADispositionResponseDto>
}
