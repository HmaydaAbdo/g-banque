/**
 * Critères de recherche dynamique côté frontend.
 * Tous les champs sont optionnels — un champ undefined/null est ignoré par le service.
 */
export interface CautionSearchDto {
  numeroCaution?: string;
  banqueId?:      number;
  natureId?:      number;
  marcheId?:      number;
  dateDePrise?:   string;
  montant?:       number;
  actif?:         boolean;
  organisme?:     string;
  page?:          number;
  size?:          number;
  sort?:          string;
}
