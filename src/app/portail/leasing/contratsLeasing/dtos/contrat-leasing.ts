import {Decimal} from "decimal.js";
import {TypeContratLeasing} from "./type-contrat-leasing";
import {ContratLeasingPhaseRequestDTO, ContratLeasingPhaseResponseDTO} from "./contrat-leasing-phase";

export interface ContratLeasingRequestDTO {
  id?: number | null;
  numeroContrat: string | null;
  banqueDePrelevementId: number | null;
  organismeLeasingId: number | null;
  immobilisation: string | null;
  montantAchatTTC: number | null;
  dateDebut: Date | null;
  periodeEnMois: number | null;
  valeurResiduelle: number | null;
  typeContrat: TypeContratLeasing | null;
  phases: ContratLeasingPhaseRequestDTO[] ;
}

export interface ContratLeasingResponseDTO {
  id: number ;
  numeroContrat: string;
  banqueDePrelevementId: number;
  nomBanqueDePrelevement: string;
  organismeLeasingId: number;
  nomOrganismeLeasing: string;
  /** ID du contrat cadre actif au moment de la signature. */
  contratCadreId: number;
  /** Plafond du contrat cadre au moment de la signature. */
  plafond: number;
  immobilisation: string;
  montantAchatTTC: number;
  dateDebut: string;
  periodeEnMois: number;
  dateFin: string;
  typeContrat: TypeContratLeasing;
  valeurResiduelle: number;
  phases?: ContratLeasingPhaseResponseDTO[];
  completed: boolean;
}
