import {Status} from "../../prefinancements/enums/Status";

export interface FraisBancaireSearchDto {
  banqueId?: number;
  libelle?: string;
  status?: Status;
}
