import {StatutEcheanceCMT} from "../enums/StatutEcheanceCMT";

export interface EcheanceCMTSearchDto {
  statutPaiement?: StatutEcheanceCMT;
  year?: number;
  month?: number;
  numeroEcheance?: number;
}
