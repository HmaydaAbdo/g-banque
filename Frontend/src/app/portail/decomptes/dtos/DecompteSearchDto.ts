import { Status } from "../../prefinancements/enums/Status";

export interface DecompteSearchDto {
  marcheId?: number;
  banqueId?: number;
  status?: Status;
  dateEmissionFrom?: Date | null;
  dateEmissionTo?: Date | null;
  dateOperationFrom?: Date | null;
  dateOperationTo?: Date | null;
  dateValeurFrom?: Date | null;
  dateValeurTo?: Date | null;
  montant?: number | null;
}
