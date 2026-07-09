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
 */
export interface BatchValidationRequestDto {
  transactionSource: TransactionSource;
  sens?: SensOperation;
  banqueId?: number;
  ids: number[];
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  tvaDeductible: number;
  rapprochementId: number;
}
