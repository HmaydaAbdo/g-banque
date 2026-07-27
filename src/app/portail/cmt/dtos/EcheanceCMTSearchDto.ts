import {StatutEcheanceCMT} from "../enums/StatutEcheanceCMT";

export interface EcheanceCMTSearchDto {
  statutPaiement?: StatutEcheanceCMT;
  numeroEcheance?: number;
  dateEcheanceDebut?: Date;
  dateEcheanceFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
}
