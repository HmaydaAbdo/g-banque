import { Decimal } from 'decimal.js';

export interface ContratLeasingPhaseRequestDTO {
  id?: number; // Optionnel pour la création, présent pour la modification
  numeroPhase: number;
  dureeMois: number;
  mensualiteTTC: Decimal;
}

export interface ContratLeasingPhaseResponseDTO {
  id: number;
  numeroPhase: number;
  dureeMois: number;
  mensualiteTTC: Decimal;
}

