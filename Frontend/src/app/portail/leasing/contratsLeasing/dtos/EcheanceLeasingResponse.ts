import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";

export interface EcheanceLeasingResponse {
  id: number;
  contratLeasingId: number;
  numeroContrat: string;
  numeroEcheance: number;
  dateEcheance: string;
  dateValeur:string
  dateOperation:string
  montantTTC: number;
  montantHT: number;
  montantTVA: number;
  statutPaiement: StatutEcheanceLeasing;
  observation: string;
  libelle:string
  dateCreation: string;
  dateModification: string;
}
