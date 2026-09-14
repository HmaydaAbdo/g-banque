import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";

export interface OperationErroneResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  dateEmission: string;
  dateValeur?: string;
  dateOperation?: string;
  status: Status;
  motif: TransactionSource;
}
