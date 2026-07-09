/**
 * Sens d'une opération de rapprochement, pour les types qui distinguent décaissement et
 * encaissement (virement, chèque, LCN, mise à disposition, virement international).
 * Utilisé uniquement par la validation en lot — voir BatchValidationRequestDto.
 */
export enum SensOperation {
  DECAISSEMENT = "DECAISSEMENT",
  ENCAISSEMENT = "ENCAISSEMENT"
}
