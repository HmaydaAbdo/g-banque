
import {PageResponse} from "../../../global/types/PageResponse";
import {EcheanceLeasingResponse} from "./EcheanceLeasingResponse";
import {Decimal} from "decimal.js";

export interface TableauAmortissement {
  contratLeasingId: number;
  numeroContrat: string;
  totalIndemniteAPayer: Decimal;
  montantTotalDejaPaye: Decimal;
  montantRestantAPayer: Decimal;
  echeances: PageResponse<EcheanceLeasingResponse>
}
