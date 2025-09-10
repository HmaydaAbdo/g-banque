
export interface LigneFonctionnelleSpecifiqueRequest {
  banqueId: number;
  natureLigneFonctionnelleId: number;
  plafond: number;
  taux: number;
  dateDepart: Date;
  dateFin: Date;
  actif?: boolean;
  marcheId: number;
}
