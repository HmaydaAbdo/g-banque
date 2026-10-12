/**
 * Retour « en cours » (ou « prévu ») d'une opération déjà validée, par la bascule manuelle de l'administrateur système :
 * si l'opération est rapprochée, sa transaction est supprimée et les soldes du rapprochement sont recalculés.
 * Ce texte complète toute confirmation de ce sens de bascule.
 */
export const AVERTISSEMENT_RETOUR_EN_COURS =
  'Si cette opération est rapprochée, sa transaction sera supprimée et les soldes du rapprochement recalculés.';
