import {Type} from "../../prefinancements/enums/Type";
import {Status} from "../../prefinancements/enums/Status";

export interface FactoringSearchDto {
  marcheId?: number;
  banqueId?: number;
  type?: Type;
  status?: Status;
}
