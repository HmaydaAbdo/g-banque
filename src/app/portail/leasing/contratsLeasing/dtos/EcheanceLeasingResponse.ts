import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";

export interface EcheanceLeasingResponse {
  id: number;
  contratLeasingId: number;
  numeroContrat: string;
  numeroEcheance: number;
  dateEcheance: Date;
  dateValeur:Date
  dateOperation:Date
  montantTTC: number;
  montantHT: number;
  montantTVA: number;
  statutPaiement: StatutEcheanceLeasing;
  observation: string;
  libelle:string
  dateCreation: Date;
  dateModification: Date;
}
