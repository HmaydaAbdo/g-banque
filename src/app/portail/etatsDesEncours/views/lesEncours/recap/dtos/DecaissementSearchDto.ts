import {TransactionSource} from "../../../../rapprochement/dtos/transactions/TransactionSource";

export interface DecaissementSearchDto {
  transactionSources?: TransactionSource[];
  observation:string;
  libelle:string
}
