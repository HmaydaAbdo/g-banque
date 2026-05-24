export interface RestitutionValidationRequest {
  idRestitution: number;
  tva: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  rapprochementId: number;
}
