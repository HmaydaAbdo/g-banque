
export interface CreditMoyenTermeRequestDto {
  banqueId: number;
  designation: string;
  nominal: number;
  periodicite: number;
  duree: number;
  echeance: number;
  taux: number;
  dateDebut: string;
}
