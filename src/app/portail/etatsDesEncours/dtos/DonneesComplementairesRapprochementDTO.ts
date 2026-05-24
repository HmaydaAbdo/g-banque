

export interface DonneesComplementairesRapprochementDTO {
  dateEmission: string;       // Date d'émission
  dateValeur: string;         // Date de valeur
  dateOperation: string;      // Date de l'opération
  observation: string;       // Commentaire
  tvaCollectee: number;       // Montant de TVA collectée
  tvaDeductible: number;      // Montant de TVA déductible
  libelle:string;
  rapprochementId: number;
}
