
export interface LigneFonctionnelleSpecifiqueRequest {
  natureLigneFonctionnelleId: number;
  plafond: number;
  taux: number;
  dateDepart: Date;
  dateFin: Date;
  actif?: boolean;
  marcheId: number;
}
