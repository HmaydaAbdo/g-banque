import { PlanBanque } from './dtos/PlanTresorerie';

/** Situation d'une banque (ou d'un total) : saine, solde négatif couvert par la facilité de caisse, ou besoin. */
export type NiveauSituation = 'ok' | 'warn' | 'danger';

export function niveauSituation(plan: PlanBanque): NiveauSituation {
  return plan.besoinFinancement > 0 ? 'danger' : plan.joursSoldeNegatif > 0 ? 'warn' : 'ok';
}

const RANG: Record<NiveauSituation, number> = { danger: 0, warn: 1, ok: 2 };

/**
 * Banques par urgence : celles en besoin d'abord (le plus gros besoin en tête), puis celles qui utilisent
 * leur facilité de caisse (le solde le plus bas en tête), puis les autres par nom.
 */
export function ordonnerParUrgence(banques: PlanBanque[]): PlanBanque[] {
  return [...banques].sort((a, b) => {
    const rang = RANG[niveauSituation(a)] - RANG[niveauSituation(b)];
    if (rang !== 0) {
      return rang;
    }
    return niveauSituation(a) === 'danger' ? b.besoinFinancement - a.besoinFinancement
      : niveauSituation(a) === 'warn' ? a.pointBas.solde - b.pointBas.solde
        : a.banqueNom.localeCompare(b.banqueNom, 'fr');
  });
}

/** Nombre de banques par situation, pour la ligne de synthèse du total. */
export function compterParSituation(banques: PlanBanque[]): Record<NiveauSituation, number> {
  const compte: Record<NiveauSituation, number> = { danger: 0, warn: 0, ok: 0 };
  banques.forEach(b => compte[niveauSituation(b)]++);
  return compte;
}

/** Nombre de jours entiers entre deux dates ISO (yyyy-MM-dd), sans fuseau horaire. */
export function joursEntre(debutIso: string, finIso: string): number {
  const jours = (iso: string) => Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) / 86_400_000;
  return Math.round(jours(finIso) - jours(debutIso));
}

/** Part de la facilité de caisse utilisée au solde le plus bas, de 0 à 1 (0 sans facilité de caisse). */
export function partFacilite(plan: PlanBanque): number {
  if (plan.plafondFC <= 0) {
    return 0;
  }
  return Math.min(1, Math.max(0, -plan.pointBas.solde) / plan.plafondFC);
}

/** Ce qu'une banque saine peut prêter sans toucher à sa facilité de caisse : son solde le plus bas, s'il est positif. */
export function excedent(plan: PlanBanque): number {
  return niveauSituation(plan) === 'ok' ? Math.max(0, plan.pointBas.solde) : 0;
}

/** Mini-courbe du solde jour par jour, tracée dans une boîte de 80 × 24. */
export function trajectoire(plan: PlanBanque): { points: string; zero: number | null } {
  const valeurs = plan.courbe.map(p => p.solde);
  if (valeurs.length < 2) {
    return { points: '', zero: null };
  }
  const min = Math.min(...valeurs);
  const max = Math.max(...valeurs);
  const echelle = max === min ? 1 : max - min;
  const y = (v: number) => 22 - ((v - min) / echelle) * 20;
  const points = valeurs.map((v, i) => `${((i / (valeurs.length - 1)) * 80).toFixed(1)},${y(v).toFixed(1)}`).join(' ');
  return { points, zero: min < 0 && max > 0 ? y(0) : null };
}
