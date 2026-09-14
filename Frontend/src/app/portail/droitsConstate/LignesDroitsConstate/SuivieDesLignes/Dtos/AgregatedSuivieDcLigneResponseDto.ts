import {PageResponse} from "../../../../global/types/PageResponse";
import {SuivieDcLigneResponseDto} from "./SuivieDcLigneResponseDto";

export interface AgregatedSuivieDcLigneResponseDto {
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: number;
  totalCredit: number;
  reste: number;
  page: PageResponse<SuivieDcLigneResponseDto>;
}
