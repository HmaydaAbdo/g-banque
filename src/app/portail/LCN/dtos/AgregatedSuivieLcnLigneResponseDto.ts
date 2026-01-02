import {PageResponse} from "../../global/types/PageResponse";
import {LcnResponseDTO} from "./LcnResponseDTO";

export interface AgregatedSuivieLcnLigneResponseDto {
  banqueName: string;
  total: number;
  page: PageResponse<LcnResponseDTO>;
}
