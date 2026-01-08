
export interface ImpotEtTaxeValidationRequest {
  idImpotEtTaxe: number;
  tva: number;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  libelle: string;
}
