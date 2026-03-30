export enum PorteeNature {
  GENERALE               = 'GENERALE',
  SPECIFIQUE             = 'SPECIFIQUE',
  GENERALE_ET_SPECIFIQUE = 'GENERALE_ET_SPECIFIQUE'
}

/** Labels d'affichage pour les dropdowns */
export const PORTEE_NATURE_LABELS: Record<PorteeNature, string> = {
  [PorteeNature.GENERALE]:               'Générale uniquement',
  [PorteeNature.SPECIFIQUE]:             'Spécifique uniquement',
  [PorteeNature.GENERALE_ET_SPECIFIQUE]: 'Générale et Spécifique',
};
