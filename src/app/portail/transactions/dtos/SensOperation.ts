/**
 * Sens d'une opération de rapprochement, pour les types qui distinguent décaissement et
 * encaissement (virement, chèque, LCN, mise à disposition, virement international).
 * Utilisé par la validation en lot (voir BatchValidationRequestDto) et par les modules dont
 * l'entité elle-même porte les deux sens sur un seul enregistrement (ex. Provision).
 */
export enum SensOperation {
  DECAISSEMENT = "DECAISSEMENT",
  ENCAISSEMENT = "ENCAISSEMENT"
}
