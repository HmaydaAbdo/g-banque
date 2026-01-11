export interface RestitutionValidationRequest {
  idRestitution: number;
  tva: number;
  dateEmission: Date;
  dateValeur: Date;
  dateOperation: Date;
  observation: string;
  libelle: string;
}
