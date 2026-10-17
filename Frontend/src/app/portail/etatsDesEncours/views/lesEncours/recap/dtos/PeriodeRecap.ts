/**
 * Horizon de projection des encours prévisionnels — doit correspondre exactement aux valeurs de
 * l'enum backend staport.finance.gBanque.recapitulatif.enums.PeriodeRecap.
 *
 * Un seul horizon partagé par l'accueil, l'écran Récapitulatifs, les rapports PDF et la
 * Trésorerie prévisionnelle. Fin de mois par défaut ; le trimestre est le trimestre civil.
 */
export type PeriodeRecap = 'A_CE_JOUR' | 'FIN_DE_MOIS' | 'FIN_DE_TRIMESTRE';

/** Horizons proposés par les sélecteurs d'écran (« À ce jour » n'a de sens que pour le Récap global). */
export type HorizonPrevision = Extract<PeriodeRecap, 'FIN_DE_MOIS' | 'FIN_DE_TRIMESTRE'>;

export const HORIZON_PAR_DEFAUT: HorizonPrevision = 'FIN_DE_MOIS';

export const HORIZON_OPTIONS: { label: string; value: HorizonPrevision }[] = [
  { label: 'Fin de mois', value: 'FIN_DE_MOIS' },
  { label: 'Fin de trimestre', value: 'FIN_DE_TRIMESTRE' },
];

/** Horizon lu dans une adresse (`?horizon=`) — toute autre valeur retombe sur la fin de mois. */
export function horizonDepuisParam(valeur: string | null | undefined): HorizonPrevision {
  return valeur === 'FIN_DE_TRIMESTRE' ? 'FIN_DE_TRIMESTRE' : HORIZON_PAR_DEFAUT;
}

/**
 * Horizon au chargement d'un écran qui le porte dans son adresse : un rechargement de la page (F5) revient toujours à
 * la fin de mois — le choix de la fin de trimestre n'est conservé que par le retour arrière ou un lien partagé.
 */
export function horizonAuDemarrage(valeur: string | null | undefined, rechargement: boolean): HorizonPrevision {
  return rechargement ? HORIZON_PAR_DEFAUT : horizonDepuisParam(valeur);
}

/**
 * Dernier jour de l'horizon à partir d'une date — même calcul que PeriodeRecap.horizon() côté
 * backend, utilisé uniquement pour l'affichage (libellés « jusqu'au 31/12 »).
 */
export function finHorizon(horizon: PeriodeRecap, aujourdHui: Date = new Date()): Date {
  if (horizon === 'A_CE_JOUR') {
    return new Date(aujourdHui.getFullYear(), aujourdHui.getMonth(), aujourdHui.getDate());
  }
  const dernierMois = horizon === 'FIN_DE_TRIMESTRE'
    ? Math.floor(aujourdHui.getMonth() / 3) * 3 + 2
    : aujourdHui.getMonth();
  return new Date(aujourdHui.getFullYear(), dernierMois + 1, 0);
}
