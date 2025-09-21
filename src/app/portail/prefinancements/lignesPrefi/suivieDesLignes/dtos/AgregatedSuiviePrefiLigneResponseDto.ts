import {PageResponse} from "../../../../global/types/PageResponse";
import {SuiviePrefiLigneResponseDto} from "./SuiviePrefiLigneResponseDto";
import {Decimal} from "decimal.js";

export interface AgregatedSuiviePrefiLigneResponseDto {
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: Decimal;
  totalCredit: Decimal;
  reste: Decimal;
  page: PageResponse<SuiviePrefiLigneResponseDto>;
}
