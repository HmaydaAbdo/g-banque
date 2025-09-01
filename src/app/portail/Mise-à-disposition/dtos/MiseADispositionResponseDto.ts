/**
 * Interface representing the data for a single MiseADisposition resource
 * returned from the API.
 */
export interface MiseADispositionResponseDto {
  numero: string;
  dateEmission: string;
  beneficiaire: string;
  montant: number;
  banqueName: string;
  cinBeneficiaire: string;
  active: boolean;
}
