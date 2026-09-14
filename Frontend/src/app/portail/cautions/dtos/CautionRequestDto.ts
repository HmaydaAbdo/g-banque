/**
 * DTO partagé pour la création et la modification d'une caution.
 *
 * Tous les champs sont modifiables — la logique de garde (actif = false)
 * est appliquée au niveau du backend.
 *
 * marcheId est optionnel — null pour dissocier la caution d'un marché.
 */
export interface CautionRequestDto {
  banqueId:      number;
  natureId:      number;
  marcheId?:     number | null;
  dateDePrise:      string;
  dateDeMainLevee?: string | null;  // optionnel — null si caution encore active
  numeroCaution: string;
  designation:   string;
  organisme:     string;
  montant:       number;
  taux:          number;
}
