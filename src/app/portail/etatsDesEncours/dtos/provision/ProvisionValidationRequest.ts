export interface ProvisionValidationRequest {
  idProvision: number;
  tauxTva: number | null;
  montantTva: number | null;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  rapprochementId: number;
}
