import {TransactionSource} from "../../transactions/dtos/TransactionSource";

export interface OperationErroneRequestDto {
  montant: number;
  banqueId: number;
  dateEmission: string;
  motif: TransactionSource;
  libelle?: string;
  observation?: string;
}
