/**
 * Interface representing the data for a single MiseADisposition resource
 * returned from the API.
 */
export interface MiseADispositionResponseDto {
  numero: string;
  dateEmission: string;
  dateOperation:string;
  dateValeur:string;
  libelle:string;
  observation:string;
  beneficiaire: string;
  montant: number;
  banqueName: string;
  cinBeneficiaire: string;
  active: boolean;
}
