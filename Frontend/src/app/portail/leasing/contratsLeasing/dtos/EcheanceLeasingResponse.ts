import {StatutEcheanceLeasing} from "./StatutEcheanceLeasing";
import {TypeMoyenPaiementAvance} from "./avance-leasing";

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
  /** Moyen de paiement qui règle l'échéance (avance uniquement), null sinon. */
  moyenPaiementType: TypeMoyenPaiementAvance | null;
  moyenPaiementId: number | null;
}
