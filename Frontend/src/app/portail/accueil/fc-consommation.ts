/** Situation d'un solde vis-à-vis du plafond de la facilité de caisse (FC). */
export type StatutBesoin = 'DISPONIBILITE' | 'BESOIN_COUVERT' | 'BESOIN_DEPASSEMENT';

/** Palier de couleur de la barre : aucun (FC non utilisée), faible (< 70 %), élevé (70 à 100 %), dépassé (> 100 %). */
export type PalierFc = 'aucun' | 'faible' | 'eleve' | 'depasse';

export const SEUIL_FC_ELEVE = 70;
export const SEUIL_FC_PLAFOND = 100;
/** Largeur minimale de la barre (en %) dès que la FC est entamée, pour qu'elle reste visible. */
export const LARGEUR_FC_MINIMALE = 2;

/** Pourcentage de consommation à l'affichage : virgule française, une décimale sous 1 % (« 0,2 »). */
export function libellePourcentage(pourcentage: number): string {
  return pourcentage.toLocaleString('fr-FR', { maximumFractionDigits: 1 });
}

export interface SituationFc {
  /** Découvert utilisé, au plus égal au plafond (0 si le solde est positif). */
  utilise: number;
  /** Part du plafond encore disponible. */
  reste: number;
  /** Consommation du plafond en pourcentage entier (une décimale sous 1 %), au-delà de 100 en cas de dépassement. */
  pourcentage: number;
  /** Largeur de la barre, bornée à 100 et d'au moins `LARGEUR_FC_MINIMALE` si la FC est entamée. */
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
  const brut = plafondNet > 0 ? (decouvert / plafondNet) * 100 : decouvert > 0 ? SEUIL_FC_PLAFOND + 1 : 0; // sans plafond : toujours un dépassement
  // Sous 1 % : une décimale, sans jamais tomber à 0 tant que la FC est entamée.
  const pourcentage = brut > 0 && brut < 1 ? Math.max(Math.round(brut * 10) / 10, 0.1) : Math.round(brut);
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
    largeur: decouvert > 0 ? Math.min(Math.max(pourcentage, LARGEUR_FC_MINIMALE), 100) : 0,
    palier,
    statut,
    montantTotal,
  };
}
