import {Decimal} from "decimal.js";
import {TransactionSource} from "../../etatsDesEncours/rapprochement/dtos/transactions/TransactionSource";
import {Status} from "../../prefinancements/enums/Status";

export interface InteretResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: Decimal;
  banqueId: number;
  banqueNom: string;
  dateEmission: Date;
  dateValeur?: Date;
  dateOperation?: Date;
  status: Status;
}
