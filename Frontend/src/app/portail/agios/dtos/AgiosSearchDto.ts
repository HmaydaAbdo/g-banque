import {Status} from "../../prefinancements/enums/Status";

export interface AgiosSearchDto {
  banqueId?: number;
  libelle?: string;
  status?: Status;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
