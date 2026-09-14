/**
 * Ligne du journal d'audit.
 *
 * Ne porte pas le détail champ-par-champ : celui-ci est chargé à la demande via
 * `LogService.getLogDetails(id)`.
 */
export interface LogResponse {
  id: number;
  timestamp: Date;
  userName: string;
  operationType: string;
  entityName: string;
  entityId: number;
}
