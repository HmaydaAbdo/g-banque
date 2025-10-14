

export interface AgiosRequestDto {
  montant: number;
  banqueId: number;
  dateEmission: string;
  libelle?: string;
  observation?: string;
}
