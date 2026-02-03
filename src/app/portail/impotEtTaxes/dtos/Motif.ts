export enum Motif {
  TVA = 'TVA',
  IR = 'IR',
  RETENUE_A_LA_SOURCE = 'RETENUE_A_LA_SOURCE',
  PENALITE_DE_RETARD = 'PENALITE_DE_RETARD',
  DGI="Direction Générale des Impôts"
}

export const MotifLabels: Record<Motif, string> = {
  [Motif.TVA]: 'TVA',
  [Motif.IR]: 'IR',
  [Motif.RETENUE_A_LA_SOURCE]: 'Retenue à la source',
  [Motif.PENALITE_DE_RETARD]: 'Pénalité de retard',
  [Motif.DGI]: 'DGI',
};
