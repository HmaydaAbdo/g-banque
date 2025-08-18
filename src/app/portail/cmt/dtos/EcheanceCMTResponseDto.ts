import {Decimal} from "decimal.js";
import {StatutEcheanceCMT} from "../enums/StatutEcheanceCMT";

export interface EcheanceCMTResponseDto {
  id: number;
  numeroEcheance: number;
  dateEcheance: string;
  montant: Decimal;
  statutPaiement: StatutEcheanceCMT;
  dateCreation: string;
  dateModification: string;
  credit: {
    id: number;
    designation: string;
  };
}
