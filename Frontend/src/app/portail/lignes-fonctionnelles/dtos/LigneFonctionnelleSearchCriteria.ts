// LigneFonctionnelleSearchCriteria.ts

export interface LigneFonctionnelleSearchCriteria {
  banqueId?: number;
  natureLigneFonctionnelleId?: number;
  yearDepart?: number;
  marcheId?: number | null;
  actif: boolean;
  isExpired?: boolean;
}
