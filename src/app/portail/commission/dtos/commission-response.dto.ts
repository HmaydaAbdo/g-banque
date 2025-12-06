import {TransactionSource} from "../../transactions/dtos/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";

export interface CommissionResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  operation: TransactionSource;
  status: Status;
  dateEmission: Date;
  dateValeur?: Date;
  dateOperation?: Date;

}
