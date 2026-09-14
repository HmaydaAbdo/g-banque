import {Decimal} from "decimal.js";

export interface TransactionUpdateRequestDto {
  dateValeur:string,
  dateOperation:string,
  tvaDeductible:Decimal,
  tvaCollecte:Decimal,

}
