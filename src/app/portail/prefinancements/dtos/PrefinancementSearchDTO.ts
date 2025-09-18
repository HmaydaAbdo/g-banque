import {Type} from "../enums/Type";
import {Status} from "../enums/Status";

/**
 * Interface for the search DTO (used for querying records).
 * Corresponds to the Java PrefinancementSearchDTO.
 */
export interface PrefinancementSearchDTO {
  marcheId?: number;
  banqueId?: number;
  type?: Type;
  status?: Status;
}
