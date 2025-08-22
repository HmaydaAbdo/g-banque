import {PageResponse} from "../../../../../../global/types/PageResponse";
import {EcheanceCMTResponseDto} from "../../../../../../cmt/dtos/EcheanceCMTResponseDto";

export interface EncoursCMTResponseDTO {
  banqueId: number;
  nomBanque: string;
  totalEncours: number;
  nombreEcheancesEnAttente: number;
  nombreCreditsConcernes: number;
  echeances: PageResponse<EcheanceCMTResponseDto>;
}
