export enum Motif {
  TVA = 'TVA',
  IR = 'IR',
  RETENUE_A_LA_SOURCE = 'RETENUE_A_LA_SOURCE',
  PENALITE_DE_RETARD = 'PENALITE_DE_RETARD',
  DGI = 'DGI',
  COMMUNAL = 'COMMUNAL',
  ADII = 'ADII',
  IS = 'IS',
  TVA_COMMISSION_CAUTION = 'TVA_COMMISSION_CAUTION',
}

export const MotifLabels: Record<Motif, string> = {
  [Motif.TVA]: 'TVA',
  [Motif.IR]: 'IR',
  [Motif.RETENUE_A_LA_SOURCE]: 'Retenue à la source',
  [Motif.PENALITE_DE_RETARD]: 'Pénalité de retard',
  [Motif.DGI]: 'Direction Générale des Impôts',
  [Motif.COMMUNAL]: 'Communal',
  [Motif.IS]: 'Impôts sur les sociétés',
  [Motif.ADII]: 'Administration des Douanes et Impôts Indirects',
  [Motif.TVA_COMMISSION_CAUTION]: 'TVA sur commission sur caution',
};
