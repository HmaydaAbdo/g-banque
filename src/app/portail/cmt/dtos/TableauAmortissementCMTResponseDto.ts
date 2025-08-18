import {PageResponse} from "../../global/types/PageResponse";
import {Decimal} from "decimal.js";
import {EcheanceCMTResponseDto} from "./EcheanceCMTResponseDto";

export interface TableauAmortissementCMTResponseDto {
  creditId: number;
  designationCredit: string;
  montantTotalAPayer: Decimal;
  montantTotalDejaPaye: Decimal;
  montantRestantAPayer: Decimal;
  echeances: PageResponse<EcheanceCMTResponseDto>;
}
