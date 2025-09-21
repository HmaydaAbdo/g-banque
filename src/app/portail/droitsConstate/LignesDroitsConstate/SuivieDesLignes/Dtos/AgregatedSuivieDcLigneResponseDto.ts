import {Decimal} from "decimal.js";
import {PageResponse} from "../../../../global/types/PageResponse";
import {SuivieDcLigneResponseDto} from "./SuivieDcLigneResponseDto";

export interface AgregatedSuivieDcLigneResponseDto {
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: Decimal;
  totalCredit: Decimal;
  reste: Decimal;
  page: PageResponse<SuivieDcLigneResponseDto>;
}
