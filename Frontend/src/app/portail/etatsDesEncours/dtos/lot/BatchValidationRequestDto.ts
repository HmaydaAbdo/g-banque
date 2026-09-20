import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../transactions/dtos/SensOperation";

/**
 * Requête de validation (rapprochement) — point d'entrée unique pour toutes les sources, qu'il
 * s'agisse de valider une seule opération (ids à un élément) ou un lot (ids à plusieurs éléments).
 * Voir conventions/architecture.md, Règle 1 (révisée le 19/09/2026).
 *
 * - sens : obligatoire pour Virement, CHEQUE, MiseADisposition, VIREMENT_INTERNATIONAL ;
 *   ignoré pour les autres types.
 * - Cas particulier LCN : pas de champ sens — utiliser directement transactionSource =
 *   LCN_DECAISSEMENT ou LCN_ENCAISSEMENT (TransactionSource.LCN n'existe que pour d'anciens
 *   enregistrements en base et n'est pas utilisé pour la validation).
 * - banqueId : obligatoire pour DECOMPTES, DROIT_CONSTATE, FACTORING, PREFINANCEMENT ;
 *   ignoré pour les autres types.
 * - montantTva, observation, libelle : propres à une opération — n'autorisés que si `ids` a un
 *   seul élément (le backend refuse sinon). Sur un vrai lot, laisser ces trois champs undefined :
 *   chaque opération conserve automatiquement les siens côté backend.
 * - dateEmission volontairement absent : le dialogue de validation unitaire la verrouille
 *   toujours sur la valeur de l'entité, jamais éditée par l'utilisateur.
 */
export interface BatchValidationRequestDto {
  transactionSource: TransactionSource;
  sens?: SensOperation;
  banqueId?: number;
  ids: number[];
  dateValeur: string;
  dateOperation: string;
  /** Taux de TVA (%), exclusif de montantTva. Sur un vrai lot, seul un taux est accepté : la
   * saisie libre d'un montant fixe appliqué a N operations de montants differents n'a pas de sens
   * comptable (voir TvaResolver cote backend et <app-tva-input [autoriserSaisieLibre]="false">). */
  tauxTva: number | null;
  /** Montant de TVA en saisie libre, exclusif de tauxTva — n'autorisé que si `ids` a un seul élément. */
  montantTva?: number | null;
  /** Observation propre à l'opération — n'autorisée que si `ids` a un seul élément. */
  observation?: string | null;
  /** Libellé propre à l'opération — n'autorisé que si `ids` a un seul élément. */
  libelle?: string | null;
  rapprochementId: number;
}
