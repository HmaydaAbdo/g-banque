import {TypeContratLeasing} from "./type-contrat-leasing";

export interface ContratLeasingSearchDTO {
  numeroContrat?: string;
  immobilisation?: string;
  typeContrat?: TypeContratLeasing;
  organismeLeasingId?: number;
  banqueDePrelevementId?: number;
  completed?: boolean;
}
