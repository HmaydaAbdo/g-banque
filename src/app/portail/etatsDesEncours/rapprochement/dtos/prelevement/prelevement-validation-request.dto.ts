export interface PrelevementValidationRequestDto {
  idPrelevement: number;
  tva: number;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
}
