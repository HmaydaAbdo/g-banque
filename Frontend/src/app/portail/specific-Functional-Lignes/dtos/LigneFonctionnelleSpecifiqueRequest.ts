
export interface LigneFonctionnelleSpecifiqueRequest {
  natureLigneFonctionnelleId: number;
  plafond: number;
  taux: number;
  dateDepart: string;
  dateFin: string;
  actif?: boolean;
  marcheId: number;
}
