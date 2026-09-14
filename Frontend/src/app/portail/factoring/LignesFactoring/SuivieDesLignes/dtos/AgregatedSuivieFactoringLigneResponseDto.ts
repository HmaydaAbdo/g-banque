import {PageResponse} from "../../../../global/types/PageResponse";
import {SuivieFactoringLineResponseDto} from "./SuivieFactoringLineResponseDto";


export interface AgregatedSuivieFactoringLigneResponseDto {
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: number;
  totalCredit: number;
  reste: number;
  page: PageResponse<SuivieFactoringLineResponseDto>;
}
