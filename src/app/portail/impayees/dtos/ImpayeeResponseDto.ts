import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import { Status } from "../../prefinancements/enums/Status";

export interface ImpayeeResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  motif: TransactionSource;
  dateEmission: Date;
  dateValeur?: Date;
  dateOperation?: Date;
  status: Status;
  dateCreation: Date;
  dateModification: Date;
}
