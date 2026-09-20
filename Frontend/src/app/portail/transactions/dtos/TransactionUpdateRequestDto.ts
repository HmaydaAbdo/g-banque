/**
 * Requête de modification d'une transaction déjà validée — mêmes champs, et la même politique
 * TVA (taux OU montant), que le dialogue de validation générique (architecture.md, Règle 2).
 * dateEmission et la banque de prélèvement ne sont volontairement pas éditables ici.
 */
export interface TransactionUpdateRequestDto {
  dateValeur: string;
  dateOperation: string;
  libelle: string;
  observation: string;
  /** Taux de TVA (%), exclusif de montantTva. */
  tauxTva: number | null;
  /** Montant de TVA en saisie libre, exclusif de tauxTva. */
  montantTva: number | null;
}
