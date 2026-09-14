import {Type} from "../enums/Type";
import {Status} from "../enums/Status";

/**
 * Interface for the response DTO (used for retrieving records).
 * Corresponds to the Java PrefinancementResponseDTO.
 */
export interface PrefinancementResponseDTO {
  id: number;
  marcheId: number;
  marcheName: string;
  marcheNumero: string;
  banqueId: number;
  banqueName: string;
  dateEmission: string;
  montant: number;
  observation: string;
  type: Type;
  dateOperation: string;
  dateValeur: string;
  status: Status;
  libelle:string
}
