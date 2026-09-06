export interface ProvisionValidationRequest {
  idProvision: number;
  tva: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  rapprochementId: number;
}
