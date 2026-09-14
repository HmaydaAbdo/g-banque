import { PageResponse } from "../../global/types/PageResponse";
import { ImpotEtTaxeResponseDto } from "./ImpotEtTaxeResponseDto";

export interface EncoursImpotEtTaxeDto {
  totalDesEncours: number;
  page: PageResponse<ImpotEtTaxeResponseDto>;
}
