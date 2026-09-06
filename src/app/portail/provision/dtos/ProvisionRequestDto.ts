import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../transactions/dtos/SensOperation";

export interface ProvisionRequestDto {
  montant: number;
  banqueId: number;
  dateEmission: string;
  motif: TransactionSource;
  sens: SensOperation;
  libelle?: string;
  observation?: string;
}
