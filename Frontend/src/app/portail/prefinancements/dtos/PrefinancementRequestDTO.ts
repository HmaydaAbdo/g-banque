import {Type} from "../enums/Type";

/**
 * Interface for the request DTO (used for creating or updating records).
 * Corresponds to the Java PrefinancementRequestDTO.
 */
export interface PrefinancementRequestDTO {
  marcheId: number;
  dateEmission: string;
  montant: number;
  type: Type;
}
