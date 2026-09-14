import {Status} from "../../prefinancements/enums/Status";

export interface RestitutionSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: number;
  status?: Status;
  dateEmissionDebut?: Date;
  dateEmissionFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
