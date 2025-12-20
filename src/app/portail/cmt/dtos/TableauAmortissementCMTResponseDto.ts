import {PageResponse} from "../../global/types/PageResponse";
import {EcheanceCMTResponseDto} from "./EcheanceCMTResponseDto";

export interface TableauAmortissementCMTResponseDto {
  creditId: number;
  designationCredit: string;
  montantTotalAPayer: number;
  montantTotalDejaPaye: number;
  montantRestantAPayer: number;
  echeances: PageResponse<EcheanceCMTResponseDto>;
}
