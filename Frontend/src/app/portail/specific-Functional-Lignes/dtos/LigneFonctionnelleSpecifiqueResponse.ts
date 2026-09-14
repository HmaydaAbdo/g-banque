export interface LigneFonctionnelleSpecifiqueResponse {
  id: number;
  banqueId: number;
  banqueNom: string;
  natureLigneFonctionnelleId: number;
  natureLigneFonctionnelleLabel: string;
  natureLigneFonctionnelleDescription: string;
  plafond: number;
  encours: number | null;
  solde: number | null;
  taux: number;
  dateDepart: string;
  dateFin: string;
  dateCreation: string;
  dateModification: string;
  actif: boolean;
  expired: boolean;
  marcheId: number;
  marcheNom: string;
  marcheNumero: string;
}
