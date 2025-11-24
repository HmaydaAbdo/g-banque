import { Decimal } from "decimal.js";
import { StatutEcheanceCMT } from "../enums/StatutEcheanceCMT";

export interface EcheanceCMTResponseDto {
  id: number;
  numeroEcheance: number;
  dateEcheance: string;
  dateValuer: string;
  dateOperation: string;
  montantTtc: number;
  observation: string;
  libelle: string;
  statutPaiement: StatutEcheanceCMT;
  dateCreation: string;
  dateModification: string;
  credit: {
    id: number;
    designation: string;
  };
}
