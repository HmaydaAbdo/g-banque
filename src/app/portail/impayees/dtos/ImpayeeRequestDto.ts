import {TransactionSource} from "../../transactions/dtos/TransactionSource";

export interface ImpayeeRequestDto {
  libelle: string;
  observation: string;
  montant: number;
  banqueEncaissementId: number;
  motif: TransactionSource;
  dateEmission: Date;
}
