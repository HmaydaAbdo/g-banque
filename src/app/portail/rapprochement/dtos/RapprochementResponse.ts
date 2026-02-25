import {TransactionResponseDto} from "../../transactions/dtos/TransactionResponseDto";
import {PageResponse} from "../../global/types/PageResponse";
import {StatutRapprochement} from "../enums/StatutRapprochement";

export interface RapprochementResponse {

  id: number;
  banqueId: number;
  banqueNom: string;
  dateRapprochement: string;

  soldeFin: number;
  soldeDebut: number;

  totalCredit: number;
  totalDebit: number;

  tvaDeductible: number;
  tvaCollectee: number;
  tvaDue: number;

  statutRapprochement: StatutRapprochement;

  transactions: PageResponse<TransactionResponseDto>;
}
