export interface CautionResponseDto {
  id: number;

  // Banque
  banqueId: number;
  banqueNom: string;

  // Nature
  natureId: number;
  natureLabel: string;
  natureCode: string;

  // Marché — optionnel
  marcheId:     number | null;
  marcheNumero: string | null;
  marcheNom:    string | null;

  // Données caution
  dateDePrise:      string;        // yyyy-MM-dd
  dateDeMainLevee:  string | null; // yyyy-MM-dd — null si caution encore active
  numeroCaution: string;
  designation:   string;
  organisme:     string;
  montant:       number;
  taux:          number;
  actif:         boolean;

  // Audit
  createdAt: string;
  updatedAt: string;
}
