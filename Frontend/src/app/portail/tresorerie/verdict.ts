import { PlanTresorerie } from './dtos/PlanTresorerie';
import { dateCourte, montantCompact } from './format';

/** Niveau de la situation : sain, solde négatif couvert par la facilité de caisse, ou besoin. */
export type NiveauVerdict = 'ok' | 'warn' | 'danger';

/** Une pastille du bandeau : une banque (ou un total) avec son montant et son jour, sans phrase. */
export interface PastilleVerdict {
  libelle: string;
  /** Teinte de la pastille ; `info` pour une mention neutre (ex. « Groupe couvert »). */
  niveau: NiveauVerdict | 'info';
}

export interface Verdict {
  niveau: NiveauVerdict;
  titre: string;
  pastilles: PastilleVerdict[];
}

const jourMois = (iso: string) => dateCourte(iso).slice(0, 5);

/**
 * État du groupe en un titre court et des pastilles : la pire banque d'abord, puis chaque banque concernée
 * avec son montant et son jour. Besoin (rouge) > solde négatif couvert (orange) > sain (vert).
 */
export function verdictDuPlan(plan: PlanTresorerie): Verdict {
  const besoins = plan.banques.filter(b => b.besoinFinancement > 0)
    .sort((a, b) => b.besoinFinancement - a.besoinFinancement);
  if (besoins.length > 0) {
    const pire = besoins[0];
    const autres = besoins.length - 1;
    const pastilles: PastilleVerdict[] = besoins.map(b => ({
      libelle: `${b.banqueNom} · −${montantCompact(b.besoinFinancement)} ${b.devise} · ${jourMois(b.pointBas.date)}`,
      niveau: 'danger',
    }));
    // Une facilité de caisse ne couvre que sa propre banque : le total peut rester positif quand une banque est en besoin
    const total = plan.groupes.find(g => g.devise === pire.devise);
    if (total && total.besoinFinancement === 0 && total.pointBas.disponibilite > 0) {
      pastilles.push({ libelle: `Groupe couvert · ${montantCompact(total.pointBas.disponibilite)} ${total.devise}`, niveau: 'info' });
    }
    return {
      niveau: 'danger',
      titre: `Besoin · ${pire.banqueNom} ${montantCompact(pire.besoinFinancement)} ${pire.devise} le ${jourMois(pire.pointBas.date)}`
        + (autres > 0 ? ` · +${autres} banque${autres > 1 ? 's' : ''}` : ''),
      pastilles,
    };
  }
  const negatives = plan.banques.filter(b => b.joursSoldeNegatif > 0)
    .sort((a, b) => a.pointBas.solde - b.pointBas.solde);
  if (negatives.length > 0) {
    return {
      niveau: 'warn',
      titre: `Facilité de caisse utilisée · ${negatives[0].banqueNom}` + (negatives.length > 1 ? ` · +${negatives.length - 1} banque${negatives.length > 2 ? 's' : ''}` : ''),
      pastilles: negatives.map(b => ({
        libelle: `${b.banqueNom} · ${montantCompact(b.pointBas.solde)} ${b.devise} · ${jourMois(b.pointBas.date)}`,
        niveau: 'warn' as const,
      })),
    };
  }
  return {
    niveau: 'ok',
    titre: `Tout va bien jusqu'au ${dateCourte(plan.dateHorizon)}`,
    pastilles: plan.groupes.map(g => ({
      libelle: `Solde le plus bas${plan.groupes.length > 1 ? ' ' + g.devise : ''} · ${montantCompact(g.pointBas.solde)} · ${jourMois(g.pointBas.date)}`,
      niveau: 'ok' as const,
    })),
  };
}
