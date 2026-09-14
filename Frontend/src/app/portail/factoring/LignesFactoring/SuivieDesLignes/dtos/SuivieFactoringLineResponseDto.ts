import {Status} from "../../../../prefinancements/enums/Status";
import {Type} from "../../../../prefinancements/enums/Type";

export interface SuivieFactoringLineResponseDto {
  id: number;
  dateEmission: string;
  dateOperation: string;
  dateValeur: string;
  libelle: string;
  debit: number;
  credit: number;
  observation: string;
  status: Status;
  type: Type;
  banqueName: string;
  marcheName: string;
  marcheNumero: string;
}
