

export interface DonneesComplementairesRapprochementDTO {
  dateEmission: Date;       // Date d'émission
  dateValeur: Date;         // Date de valeur
  dateOperation: Date;      // Date de l'opération
  observation: string;       // Commentaire
  tvaCollectee: number;       // Montant de TVA collectée
  tvaDeductible: number;      // Montant de TVA déductible
}
