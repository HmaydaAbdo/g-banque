export interface LigneFonctionnelleResponse {
  id: number;
  banqueId: number;
  banqueNom: string;
  natureLigneFonctionnelleId: number;
  natureLigneFonctionnelleLabel: string;
  natureLigneFonctionnelleDescription: string;
  plafond: number;
  taux: number;
  dateDepart: Date;
  dateFin: Date;
  dateCreation: Date;
  dateModification: Date;
  actif?: boolean;
}
