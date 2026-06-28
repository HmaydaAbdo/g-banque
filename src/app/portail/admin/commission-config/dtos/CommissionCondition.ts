export enum CommissionCondition {
  DEFAULT      = 'DEFAULT',
  SI_MATCH     = 'SI_MATCH',
  SI_NON_MATCH = 'SI_NON_MATCH'
}

export const CommissionConditionLabels: Record<CommissionCondition, string> = {
  [CommissionCondition.DEFAULT]:      'Par défaut',
  [CommissionCondition.SI_MATCH]:     'Si organisme correspond',
  [CommissionCondition.SI_NON_MATCH]: 'Si organisme ne correspond pas'
};
