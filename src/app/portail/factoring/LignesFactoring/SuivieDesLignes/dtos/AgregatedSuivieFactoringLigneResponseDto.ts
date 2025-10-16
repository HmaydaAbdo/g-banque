import {Decimal} from "decimal.js";
import {PageResponse} from "../../../../global/types/PageResponse";
import {SuivieFactoringLineResponseDto} from "./SuivieFactoringLineResponseDto";


export interface AgregatedSuivieFactoringLigneResponseDto {
  BanqueName: string;
  marcheName: string;
  marcheNumero: string;
  totalDebit: Decimal;
  totalCredit: Decimal;
  reste: Decimal;
  page: PageResponse<SuivieFactoringLineResponseDto>;
}
