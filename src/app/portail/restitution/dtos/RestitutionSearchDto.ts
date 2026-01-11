import {Status} from "../../prefinancements/enums/Status";

export interface RestitutionSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: number;
  status?: Status;
}
