export interface CommissionValidationDto {
  idCommission: number;
  dateEmission: string;
  libelle: string;
  observation: string;
  dateValeur: string;
  dateOperation: string;
  tauxTva: number | null;
  montantTva: number | null;
  rapprochementId: number;
}
