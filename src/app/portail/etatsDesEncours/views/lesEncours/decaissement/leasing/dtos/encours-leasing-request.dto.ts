import {PeriodeEncours} from "./PeriodeEncours";

export interface EncoursLeasingRequestDTO {
  banqueId: number;
  periode: PeriodeEncours;
  organismeLeasingId?: number; // Le '?' rend le champ optionnel
  numeroContrat:string
  page?: number;
  size?: number;
  sort?: string;
}
