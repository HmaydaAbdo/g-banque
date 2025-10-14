
export interface EncoursLeasingRequestDTO {
  banqueId: number;
  organismeLeasingId?: number; // Le '?' rend le champ optionnel
  numeroContrat:string
  page?: number;
  size?: number;
  sort?: string;
}
