import {Status} from "../../prefinancements/enums/Status";

export interface DecompteSearchDto {
  marcheId?: number;
  banqueId?: number;
  status?: Status;
}
