export interface LeasingConfigResponse {
  id: number;
  valeurResiduelle: number;
  tauxTva: number;
  alerteEcheanceJoursCourtTerme: number;
  alerteEcheanceJoursLongTerme: number;
  dateModification: string;
}
