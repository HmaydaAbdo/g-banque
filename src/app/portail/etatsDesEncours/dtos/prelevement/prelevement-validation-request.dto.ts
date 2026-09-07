export interface PrelevementValidationRequestDto {
  idPrelevement: number;
  /**
   * Saisie de la TVA : `tauxTva` et `montantTva` sont mutuellement exclusifs — exactement l'un des
   * deux est renseigné. La conversion taux → montant reste serveur (`TvaResolver`) ; le frontend
   * ne transmet jamais un montant dérivé d'un taux.
   */
  tauxTva: number | null;
  montantTva: number | null;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  observation: string;
  libelle: string;
  rapprochementId: number;
}
