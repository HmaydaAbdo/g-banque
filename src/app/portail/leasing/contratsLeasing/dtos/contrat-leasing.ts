import {Decimal} from "decimal.js";
import {TypeContratLeasing} from "./type-contrat-leasing";
import {ContratLeasingPhaseRequestDTO, ContratLeasingPhaseResponseDTO} from "./contrat-leasing-phase";

export interface ContratLeasingRequestDTO {
  id?: number | null;
  numeroContrat: string | null;
  banqueDePrelevementId: number | null;
  organismeLeasingId: number | null;
  immobilisation: string | null;
  montantAchatTTC: Decimal | null;
  dateDebut: Date | null;
  periodeEnMois: number | null;
  valeurResiduelle: Decimal | null;
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
  immobilisation: string;
  montantAchatTTC: Decimal;
  dateDebut: Date;
  periodeEnMois: number;
  typeContrat: TypeContratLeasing;
  valeurResiduelle?: Decimal;
  phases?: ContratLeasingPhaseResponseDTO[];
  completed: boolean;
}
