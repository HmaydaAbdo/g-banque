import {Decimal} from "decimal.js";
import {Status} from "../../prefinancements/enums/Status";

export interface PrelevementResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: Decimal;
  banqueId: number;
  banqueNom: string;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  status: Status;
}
