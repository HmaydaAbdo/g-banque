
export interface LigneFonctionnelleRequest {
  banqueId: number;
  natureLigneFonctionnelleId: number; // New: ID of the NatureLigneFonctionnelle entity
  plafond: number;
  taux: number;
  dateDepart: Date;
  dateFin: Date;
  actif?: boolean;
}
