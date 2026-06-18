import { Status } from "../../prefinancements/enums/Status";

export interface DecompteSearchDto {
  marcheId?: number;
  banqueId?: number;
  status?: Status;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
  montantMin?: number | null;
  montantMax?: number | null;
}
