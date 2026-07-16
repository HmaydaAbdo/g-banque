export enum TypeChargeFixe {
  FRAIS_BANCAIRE = 'FRAIS_BANCAIRE',
  IMPOT_TAXE     = 'IMPOT_TAXE',
  AGIOS          = 'AGIOS',
  PRELEVEMENT    = 'PRELEVEMENT'
}

export const TypeChargeFixeLabels: Record<TypeChargeFixe, string> = {
  [TypeChargeFixe.FRAIS_BANCAIRE]: 'Frais bancaire',
  [TypeChargeFixe.IMPOT_TAXE]:     'Impôt / Taxe',
  [TypeChargeFixe.AGIOS]:          'Agios',
  [TypeChargeFixe.PRELEVEMENT]:    'Prélèvement'
};
