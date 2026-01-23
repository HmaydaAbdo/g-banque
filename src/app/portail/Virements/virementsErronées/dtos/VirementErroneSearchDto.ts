import {Status} from "../../../prefinancements/enums/Status";

export interface VirementErroneSearchDto {
  banqueId?: number;
  libelle?: string;
  montant?: number;
  status?: Status;
}
