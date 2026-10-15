import { PlanBanque, PlanTresorerie } from './dtos/PlanTresorerie';
import { dateCourte, montantComplet } from './format';

/** Niveau de la situation : sain, solde négatif couvert par la facilité de caisse, ou besoin de financement. */
export type NiveauVerdict = 'ok' | 'warn' | 'danger';

export interface Verdict {
  niveau: NiveauVerdict;
  titre: string;
  detail: string;
}

/**
 * Réponse en une phrase à « est-ce que ça va ? » pour tout le groupe : la pire banque d'abord.
 * Besoin de financement (rouge) > solde négatif couvert (orange) > sain (vert).
 */
export function verdictDuPlan(plan: PlanTresorerie): Verdict {
  const horizon = dateCourte(plan.dateHorizon);
  const besoins = plan.banques.filter(b => b.besoinFinancement > 0)
    .sort((a, b) => b.besoinFinancement - a.besoinFinancement);
  if (besoins.length > 0) {
    const pire = besoins[0];
    return {
      niveau: 'danger',
      titre: `Il manquera ${montantComplet(pire.besoinFinancement, pire.devise)} le ${dateCourte(pire.pointBas.date)} sur ${pire.banqueNom}`,
      detail: besoins.length > 1
        ? `${besoins.length} banques ont un besoin de financement avant le ${horizon}, même en utilisant toute la facilité de caisse.`
        : `Même en utilisant toute la facilité de caisse. Prévoir un financement ou décaler des sorties avant le ${horizon}.`,
    };
  }
  const negatives = plan.banques.filter(b => b.joursSoldeNegatif > 0)
    .sort((a, b) => a.pointBas.solde - b.pointBas.solde);
  if (negatives.length > 0) {
    const pire: PlanBanque = negatives[0];
    return {
      niveau: 'warn',
      titre: `${pire.banqueNom} passera sous zéro, couverte par la facilité de caisse`,
      detail: `Point bas de ${montantComplet(pire.pointBas.solde, pire.devise)} le ${dateCourte(pire.pointBas.date)}`
        + (negatives.length > 1 ? `, et ${negatives.length - 1} autre(s) banque(s) concernée(s).` : '.'),
    };
  }
  return {
    niveau: 'ok',
    titre: `Tout va bien jusqu'au ${horizon}${plan.certainesSeulement ? ' (opérations certaines)' : ''}`,
    detail: plan.groupes.map(g => `Point bas${plan.groupes.length > 1 ? ' en ' + g.devise : ' du groupe'} : `
      + `${montantComplet(g.pointBas.solde, g.devise)} le ${dateCourte(g.pointBas.date)}`).join(' · ') + '.',
  };
}
