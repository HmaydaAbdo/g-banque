
import {PageResponse} from "../../../global/types/PageResponse";
import {EcheanceLeasingResponse} from "./EcheanceLeasingResponse";
import {Decimal} from "decimal.js";

export interface TableauAmortissement {
  contratLeasingId: number;
  numeroContrat: string;
  totalIndemniteAPayer: number;
  montantTotalDejaPaye: number;
  montantRestantAPayer: number;
  echeances: PageResponse<EcheanceLeasingResponse>
}
