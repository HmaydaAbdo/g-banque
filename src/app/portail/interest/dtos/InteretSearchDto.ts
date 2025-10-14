import {Status} from "../../prefinancements/enums/Status";

export interface InteretSearchDto {
  banqueId?: number;
  libelle?: string;
  status?: Status;
}
