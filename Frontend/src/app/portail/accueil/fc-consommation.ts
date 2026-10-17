/** Situation d'un solde vis-à-vis du plafond de la facilité de caisse (FC). */
export type StatutBesoin = 'DISPONIBILITE' | 'BESOIN_COUVERT' | 'BESOIN_DEPASSEMENT';

/** Palier de couleur de la barre : aucun (FC non utilisée), faible (< 70 %), élevé (70 à 100 %), dépassé (> 100 %). */
export type PalierFc = 'aucun' | 'faible' | 'eleve' | 'depasse';

export const SEUIL_FC_ELEVE = 70;
export const SEUIL_FC_PLAFOND = 100;

export interface SituationFc {
  /** Découvert utilisé, au plus égal au plafond (0 si le solde est positif). */
  utilise: number;
  /** Part du plafond encore disponible. */
  reste: number;
  /** Consommation du plafond en pourcentage entier, au-delà de 100 en cas de dépassement. */
  pourcentage: number;
  /** Largeur de la barre, bornée à 100. */
  largeur: number;
  palier: PalierFc;
  statut: StatutBesoin;
  /** Montant porté par la ligne de total : disponibilité (solde + plafond), découvert couvert, ou dépassement. */
  montantTotal: number;
}

/**
 * Lit un solde (actuel ou prévu) au regard du plafond de FC :
 * – solde positif : disponibilité (solde + plafond), FC intacte ;
 * – solde négatif couvert par le plafond : besoin couvert par la FC, pour le montant du découvert ;
 * – solde négatif au-delà du plafond : besoin en dépassement, pour la part non couverte.
 * Sans plafond, tout solde négatif est un dépassement.
 */
export function situationFc(solde: number, plafond: number | null | undefined): SituationFc {
  const plafondNet = plafond && plafond > 0 ? plafond : 0;
  const decouvert = solde < 0 ? -solde : 0;
  const pourcentage = plafondNet > 0 ? Math.round((decouvert / plafondNet) * 100) : decouvert > 0 ? SEUIL_FC_PLAFOND + 1 : 0; // sans plafond : toujours un dépassement
  const disponibilite = solde + plafondNet;

  let statut: StatutBesoin;
  let montantTotal: number;
  if (solde >= 0) {
    statut = 'DISPONIBILITE';
    montantTotal = disponibilite;
  } else if (disponibilite >= 0) {
    statut = 'BESOIN_COUVERT';
    montantTotal = decouvert;
  } else {
    statut = 'BESOIN_DEPASSEMENT';
    montantTotal = -disponibilite;
  }

  const palier: PalierFc = decouvert === 0
    ? 'aucun'
    : pourcentage > SEUIL_FC_PLAFOND ? 'depasse' : pourcentage >= SEUIL_FC_ELEVE ? 'eleve' : 'faible';

  return {
    utilise: Math.min(decouvert, plafondNet),
    reste: Math.max(plafondNet - decouvert, 0),
    pourcentage,
    largeur: Math.min(pourcentage, 100),
    palier,
    statut,
    montantTotal,
  };
}
