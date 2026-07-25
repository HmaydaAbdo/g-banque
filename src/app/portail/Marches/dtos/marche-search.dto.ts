export interface MarcheSearchDTO {
  nom?: string;
  numero?: string;
  maitreDouvrage?: string;
  banqueId?: number;
  /** undefined/null = tous statuts, true = actifs uniquement, false = archivés uniquement. */
  actif?: boolean | null;
  dateDebutDesTravauxFrom?: Date | null;
  dateDebutDesTravauxTo?: Date | null;
  dateDAchievementDesTravauxFrom?: Date | null;
  dateDAchievementDesTravauxTo?: Date | null;
}
