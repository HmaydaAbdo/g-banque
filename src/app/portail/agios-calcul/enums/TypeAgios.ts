export enum TypeAgios {
  FACILITE_CAISSE = 'FACILITE_CAISSE',
  PREFINANCEMENT  = 'PREFINANCEMENT',
  // Futurs types :
  // ESCOMPTE = 'ESCOMPTE',
  // DECOUVERT_AUTORISE = 'DECOUVERT_AUTORISE',
}

export const TypeAgiosLabels: Record<TypeAgios, string> = {
  [TypeAgios.FACILITE_CAISSE]: 'Facilité de Caisse',
  [TypeAgios.PREFINANCEMENT]:  'Préfinancement',
};
