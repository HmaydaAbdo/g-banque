import { PageResponse } from "../../global/types/PageResponse";
import { ProvisionResponseDto } from "./ProvisionResponseDto";

export interface EncoursProvisionDto {
  totalDesEncours: number;
  page: PageResponse<ProvisionResponseDto>;
}
