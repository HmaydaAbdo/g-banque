/** Un filtre qui s'écarte de la vue par défaut, montré en pastille avec son bouton de retrait. */
export interface FiltreActif {
  cle: string;
  libelle: string;
  valeur: string;
  retirer: () => void;
}
