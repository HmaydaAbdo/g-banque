import {TransactionSource} from "../../transactions/dtos/TransactionSource";

export interface ImpayeeRequestDto {
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  motif: TransactionSource;
  dateEmission: Date;
}
