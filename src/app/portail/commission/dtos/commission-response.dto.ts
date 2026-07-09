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
  dateEmission: string;
  dateValeur?: string;
  dateOperation?: string;

  /** Caution d'origine — absente pour les commissions non générées depuis une caution. */
  cautionId?: number;
  cautionMontant?: number;
  natureId?: number;
  natureLabel?: string;
}
