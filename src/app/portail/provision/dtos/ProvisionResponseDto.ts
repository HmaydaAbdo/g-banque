import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../transactions/dtos/SensOperation";
import {Status} from "../../prefinancements/enums/Status";

export interface ProvisionResponseDto {
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
  sens: SensOperation;
}
