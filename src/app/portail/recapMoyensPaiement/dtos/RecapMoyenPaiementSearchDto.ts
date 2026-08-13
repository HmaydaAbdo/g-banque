import { TransactionSource } from '../../transactions/dtos/TransactionSource';
import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

/**
 * Critères de recherche pour les écrans Recap Encaissement / Recap Décaissement des moyens de
 * paiement. Les plages de dates portent sur la date d'opération et la date de valeur (pas la
 * date d'émission — choix validé avec HMAYDA le 31/07/2026). La recherche par montant est un
 * champ unique (correspondance exacte), pas un intervalle — choix validé avec HMAYDA le
 * 13/08/2026.
 */
export interface RecapMoyenPaiementSearchDto {
  banqueId: number | null;
  sources: TransactionSource[] | null;
  statuts: StatutMoyenPaiement[] | null;
  libelle: string | null;
  numero: string | null;
  montant: number | null;
  dateOperationFrom: Date | null;
  dateOperationTo: Date | null;
  dateValeurFrom: Date | null;
  dateValeurTo: Date | null;
}
