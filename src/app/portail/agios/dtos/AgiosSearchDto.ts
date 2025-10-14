import {Status} from "../../prefinancements/enums/Status";

export interface AgiosSearchDto {
  banqueId?: number;
  libelle?: string;
  status?: Status;
}
