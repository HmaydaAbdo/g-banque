export interface LogEntryDetailResponse {
  /**
   * Nul pour un détail reconstruit à la demande depuis Envers (il ne
   * correspond à aucune ligne persistée) — renseigné uniquement pour les
   * détails stockés des logs antérieurs à la migration.
   */
  id: number | null;
  fieldName: string;
  /** Nul lorsque le champ n'existait pas avant l'opération (création). */
  oldValue: string | null;
  /** Nul lorsque le champ n'existe plus après l'opération (suppression). */
  newValue: string | null;
}
