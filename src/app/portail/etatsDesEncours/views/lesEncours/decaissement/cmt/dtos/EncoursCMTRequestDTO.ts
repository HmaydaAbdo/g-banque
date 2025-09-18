import {PeriodeEncoursCMT} from "./PeriodeEncoursCMT";

export interface EncoursCMTRequestDTO {
  banqueId: number;
  periode: PeriodeEncoursCMT;
  designation:string
  page?: number;
  size?: number;
  sort?: string;
}
