import {StatutRapprochement} from "../enums/StatutRapprochement";

export interface RapprochementSearchDto {

  banqueId?: number ;
  dateRapprochement?: string ;
  statutRapprochement:StatutRapprochement;
}
