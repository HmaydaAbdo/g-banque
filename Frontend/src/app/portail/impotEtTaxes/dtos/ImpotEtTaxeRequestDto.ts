import { Decimal } from "decimal.js";
import {Motif} from "./Motif";

export interface ImpotEtTaxeRequestDto {
  libelle: string;
  observation: string;
  montant: Decimal;
  banqueId: number;
  motif: Motif;
  dateEmission: string;
}
