// LigneFonctionnelleSearchCriteria.ts

export interface LigneFonctionnelleSearchCriteria {
  banqueId?: number;
  natureLigneFonctionnelleId?: number;
  yearDepart?: number;
  actif: boolean;
  isExpired?: boolean;
}
