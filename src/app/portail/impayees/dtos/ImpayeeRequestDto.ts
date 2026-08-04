import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {Type} from "../../Virements/Type";

export interface ImpayeeRequestDto {
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  type: Type;
  motif: TransactionSource;
  numero?: string;
  dateEmission: string;
}
