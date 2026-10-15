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
    const autres = besoins.length - 1;
    // Toutes les banques en besoin sont nommées, avec leur montant et leur jour : pas seulement la pire
    const liste = besoins.map(b => `${b.banqueNom} : ${montantComplet(b.besoinFinancement, b.devise)} le ${dateCourte(b.pointBas.date)}`).join(' · ');
    // Une facilité de caisse ne couvre que sa propre banque : le total peut rester positif quand une banque est en besoin
    const total = plan.groupes.find(g => g.devise === pire.devise);
    const groupeCouvert = total !== undefined && total.besoinFinancement === 0 && total.pointBas.disponibilite > 0;
    return {
      niveau: 'danger',
      titre: `Il manquera ${montantComplet(pire.besoinFinancement, pire.devise)} le ${dateCourte(pire.pointBas.date)} sur ${pire.banqueNom}`
        + (autres > 0 ? ` et ${autres} autre${autres > 1 ? 's' : ''} banque${autres > 1 ? 's' : ''}` : ''),
      detail: `${liste}. Même en utilisant toute la facilité de caisse de chaque banque.`
        + (groupeCouvert
          ? ` Le total du groupe reste couvert (disponibilité la plus basse : ${montantComplet(total.pointBas.disponibilite, total.devise)}) :`
            + ' un virement depuis une banque excédentaire peut combler ce besoin.'
          : ` Prévoir un financement ou décaler des décaissements avant le ${horizon}.`),
    };
  }
  const negatives = plan.banques.filter(b => b.joursSoldeNegatif > 0)
    .sort((a, b) => a.pointBas.solde - b.pointBas.solde);
  if (negatives.length > 0) {
    const pire: PlanBanque = negatives[0];
    return {
      niveau: 'warn',
      titre: `Le solde de ${pire.banqueNom} passera en négatif, couvert par la facilité de caisse`
        + (negatives.length > 1 ? ` (${negatives.length} banques concernées)` : ''),
      detail: negatives.map(b => `${b.banqueNom} : solde le plus bas ${montantComplet(b.pointBas.solde, b.devise)} le ${dateCourte(b.pointBas.date)}`).join(' · ') + '.',
    };
  }
  return {
    niveau: 'ok',
    titre: `Tout va bien jusqu'au ${horizon}`,
    detail: plan.groupes.map(g => `Solde le plus bas${plan.groupes.length > 1 ? ' en ' + g.devise : ''} : `
      + `${montantComplet(g.pointBas.solde, g.devise)} le ${dateCourte(g.pointBas.date)}`).join(' · ') + '.',
  };
}
