export interface VirementErroneRequestDto {
  montant: number;
  banqueId: number;
  dateEmission: string;
  libelle?: string;
  observation?: string;
}
