import {PeriodeEncoursCMT} from "./PeriodeEncoursCMT";

export interface EncoursCMTRequestDTO {
  banqueId: number;
  periode: PeriodeEncoursCMT;
  page?: number;
  size?: number;
  sort?: string;
}
