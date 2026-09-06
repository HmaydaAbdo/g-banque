import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";

/**
 * Critères de recherche de l'écran Récapitulatif — encaissements et décaissements prévisionnels
 * du mois, réunis dans un seul écran depuis le 06/09/2026 (ce DTO s'appelait
 * DecaissementSearchDto.ts du temps des deux écrans séparés).
 */
export interface RecapitulatifSearchDto {
  transactionSources?: TransactionSource[];
  observation: string;
  libelle: string;
  /**
   * Filtre optionnel sur le sens de l'opération. `null`/`undefined` (valeur par défaut, « Tous »
   * côté écran) signifie « aucun filtre » — les deux sens sont retournés, entrelacés.
   */
  sens?: SensOperation | null;
  /**
   * Borne inférieure optionnelle sur la date de référence de chaque opération. Indépendante de
   * `dateReferenceJusqua` — aucune des deux n'est requise pour que l'autre s'applique.
   */
  dateReferenceDepuis?: Date | null;
  /**
   * Borne supérieure optionnelle sur la date de référence de chaque opération (colonne
   * "dateReference" côté vue SQL — déjà mappée par source : date d'émission, date d'échéance...).
   * Si non renseignée, le backend retombe sur la fin du mois en cours.
   */
  dateReferenceJusqua?: Date | null;
  /**
   * Filtre optionnel sur le montant exact de l'opération (égalité stricte, pas une plage).
   */
  montant?: number | null;
}
