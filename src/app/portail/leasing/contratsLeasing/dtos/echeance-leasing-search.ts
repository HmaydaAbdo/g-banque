import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";

export interface EcheanceLeasingSearchDTO {
  contratLeasingId?: number;
  numeroEcheance?: number;
  month?: number;
  year?: number;
  statutPaiement?: StatutEcheanceLeasing;

}
