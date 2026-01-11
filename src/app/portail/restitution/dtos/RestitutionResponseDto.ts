import {Status} from "../../prefinancements/enums/Status";

export interface RestitutionResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  status: Status;
}
