import { Status } from "../../prefinancements/enums/Status";
import {Motif} from "./Motif";

export interface ImpotEtTaxeResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  motif: Motif;
  status: Status;
  dateEmission: string;
  dateValeur?: string;
  dateOperation?: string;
}
