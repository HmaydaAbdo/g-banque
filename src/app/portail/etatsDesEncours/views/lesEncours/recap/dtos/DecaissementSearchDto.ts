import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";

export interface DecaissementSearchDto {
  transactionSources?: TransactionSource[];
  observation:string;
  libelle:string
}
