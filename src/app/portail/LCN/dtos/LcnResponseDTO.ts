export interface LcnResponseDTO {
  numero: string;
  dateEmission: string;
  beneficiaire: string;
  dateEcheance: string;
  dateOperation:string
  dateValeur:string
  libelle:string
  observation:string
  montant: number;
  active: boolean;
  banqueName: string;
}
