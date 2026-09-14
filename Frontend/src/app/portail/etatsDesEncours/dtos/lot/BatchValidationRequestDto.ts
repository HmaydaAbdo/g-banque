import {TransactionSource} from "../../../transactions/dtos/TransactionSource";
import {SensOperation} from "../../../transactions/dtos/SensOperation";

/**
 * Requête de validation (rapprochement) en lot : applique les mêmes champs communs à un
 * ensemble d'opérations de même type (et même sens pour les types qui distinguent
 * décaissement/encaissement).
 *
 * - sens : obligatoire pour Virement, CHEQUE, MiseADisposition, VIREMENT_INTERNATIONAL ;
 *   ignoré pour les autres types.
 * - Cas particulier LCN : pas de champ sens — utiliser directement transactionSource =
 *   LCN_DECAISSEMENT ou LCN_ENCAISSEMENT (TransactionSource.LCN n'existe que pour d'anciens
 *   enregistrements en base et n'est pas utilisé pour la validation).
 * - banqueId : obligatoire pour DECOMPTES, DROIT_CONSTATE, FACTORING, PREFINANCEMENT ;
 *   ignoré pour les autres types.
 * - Volontairement absent : libellé, observation, date d'émission. Propres à chaque opération,
 *   ils ne sont jamais saisis en lot — chaque opération conserve automatiquement les siens côté
 *   backend. Seuls TVA, date opération et date valeur (issus du relevé bancaire traité) sont
 *   communs à tout le lot.
 */
export interface BatchValidationRequestDto {
  transactionSource: TransactionSource;
  sens?: SensOperation;
  banqueId?: number;
  ids: number[];
  dateValeur: string;
  dateOperation: string;
  /** Taux de TVA (%). Le mode lot ne transporte jamais qu'un taux : la saisie libre d'un
   * montant fixe appliqué a N operations de montants differents n'a pas de sens comptable
   * (voir TvaResolver cote backend et <app-tva-input [autoriserSaisieLibre]="false">). */
  tauxTva: number | null;
  rapprochementId: number;
}
