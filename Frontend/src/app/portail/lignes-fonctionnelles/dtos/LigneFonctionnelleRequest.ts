import { SousLigneRequestDto } from "./SousLigneRequestDto";

export interface LigneFonctionnelleRequest {
  banqueId: number;
  natureLigneFonctionnelleId: number;
  /** Liste des IDs des marchés associés. Vide = ligne générale sans marché. */
  marcheIds: number[];
  plafond: number;
  taux: number;
  dateDepart: string;
  dateFin: string;
  sousLignes?: SousLigneRequestDto[] | null;
}
