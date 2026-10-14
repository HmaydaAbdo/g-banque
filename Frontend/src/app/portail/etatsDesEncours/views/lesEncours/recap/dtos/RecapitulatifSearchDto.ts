import {TransactionSource} from "../../../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../../../transactions/dtos/SensOperation";
import {Motif} from "../../../../../impotEtTaxes/dtos/Motif";
import {PeriodeRecap} from "./PeriodeRecap";

/**
 * Critères de recherche de l'écran Récapitulatif — encaissements et décaissements prévisionnels
 * du mois, réunis dans un seul écran depuis le 06/09/2026 (ce DTO s'appelait
 * DecaissementSearchDto.ts du temps des deux écrans séparés).
 */
export interface RecapitulatifSearchDto {
  transactionSources?: TransactionSource[];
  /**
   * Un seul champ de recherche textuelle, testé côté serveur sur libellé ET observation (OR,
   * insensible à la casse, sous-chaîne) — fusionné le 21/09/2026 depuis deux champs séparés.
   */
  libelleOuObservation: string;
  /**
   * Filtre optionnel sur le numéro de pièce (recherche partielle, insensible à la casse).
   * Sans effet sur les sources qui n'en portent pas — elles sont alors exclues du résultat.
   */
  numero?: string;
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
   * Si non renseignée, le backend retombe sur l'horizon `periode`.
   */
  dateReferenceJusqua?: Date | null;
  /**
   * Horizon de l'écran : fin de mois (défaut backend si absent) ou fin de trimestre.
   */
  periode?: PeriodeRecap;
  /**
   * Filtre optionnel sur le montant exact de l'opération (égalité stricte, pas une plage).
   */
  montant?: number | null;
  /**
   * Filtre optionnel exact sur le motif (type) d'une Commission — sans effet sur les lignes des
   * autres sources (21/09/2026, restaure le filtre « Motif » de l'ancien écran dédié).
   */
  commissionOperation?: TransactionSource | null;
  /**
   * Filtre optionnel exact sur le motif d'un Impôt/Taxe — sans effet sur les lignes des autres
   * sources (21/09/2026, restaure le filtre « Motif » de l'ancien écran dédié).
   */
  impotEtTaxeMotif?: Motif | null;
}
