import {PageResponse} from "../../global/types/PageResponse";
import {EcheanceCMTResponseDto} from "./EcheanceCMTResponseDto";

export interface EncoursCMTResponseDTO {
  banqueId: number;
  nomBanque: string;
  totalEncours: number;
  echeances: PageResponse<EcheanceCMTResponseDto>;
}
