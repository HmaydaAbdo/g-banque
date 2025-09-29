import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";
import {Decimal} from "decimal.js";

export interface EcheanceLeasingResponse {
  id: number;
  contratLeasingId: number;
  numeroContrat: string;
  numeroEcheance: number;
  dateEcheance: Date;
  dateValeur:Date
  dateOperation:Date
  montantTTC: Decimal;
  montantHT: Decimal;
  montantTVA: Decimal;
  statutPaiement: StatutEcheanceLeasing;
  observation: string;
  libelle:string
  dateCreation: Date;
  dateModification: Date;
}
