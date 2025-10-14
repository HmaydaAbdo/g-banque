import {PageResponse} from "../../../global/types/PageResponse";
import {EcheanceLeasingResponse} from "./EcheanceLeasingResponse";


export interface EncoursLeasingResponseDTO {
  banqueId: number;
  nomBanque: string;
  totalEncours: number;
  nombreEcheancesEnAttente: number;
  nombreContratsConcernes: number;
  echeances: PageResponse<EcheanceLeasingResponse>;
}
