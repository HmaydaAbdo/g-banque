import {Type} from "../../prefinancements/enums/Type";
import {Status} from "../../prefinancements/enums/Status";

export interface DroitConstateSearchDto {
  marcheId?: number;
  banqueId?: number;
  type?: Type;
  status?: Status;
  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
}
