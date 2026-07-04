export enum TypeAgios {
  FACILITE_CAISSE = 'FACILITE_CAISSE',
  PREFINANCEMENT  = 'PREFINANCEMENT',
  DROIT_CONSTATE  = 'DROIT_CONSTATE',
  FACTORING       = 'FACTORING',
}

export const TypeAgiosLabels: Record<TypeAgios, string> = {
  [TypeAgios.FACILITE_CAISSE]: 'Facilité de Caisse',
  [TypeAgios.PREFINANCEMENT]:  'Préfinancement',
  [TypeAgios.DROIT_CONSTATE]:  'Droits Constatés',
  [TypeAgios.FACTORING]:       'Factoring',
};
