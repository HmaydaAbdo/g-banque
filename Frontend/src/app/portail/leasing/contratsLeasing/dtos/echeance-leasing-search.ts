import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";

export interface EcheanceLeasingSearchDTO {
  contratLeasingId?: number;
  numeroEcheance?: number;
  dateEcheanceDebut?: Date;
  dateEcheanceFin?: Date;
  dateOperationDebut?: Date;
  dateOperationFin?: Date;
  dateValeurDebut?: Date;
  dateValeurFin?: Date;
  statutPaiement?: StatutEcheanceLeasing;

}
