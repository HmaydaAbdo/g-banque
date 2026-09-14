import {PageResponse} from "../../../../global/types/PageResponse";
import {SuiviePrefiLigneResponseDto} from "./SuiviePrefiLigneResponseDto";

export interface AgregatedSuiviePrefiLigneResponseDto {
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: number;
  totalCredit: number;
  reste: number;
  page: PageResponse<SuiviePrefiLigneResponseDto>;
}
