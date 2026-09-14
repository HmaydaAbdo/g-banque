import {Status} from "../../prefinancements/enums/Status";

export interface RestitutionResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  status: Status;
}
