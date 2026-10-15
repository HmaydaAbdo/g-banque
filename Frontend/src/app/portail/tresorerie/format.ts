/** Montant compact pour une case de grille : « 1,2 M », « 123 k », « −45 k », « 800 ». */
export function montantCompact(valeur: number): string {
  const abs = Math.abs(valeur);
  const signe = valeur < 0 ? '−' : '';
  if (abs >= 1_000_000) {
    return `${signe}${(abs / 1_000_000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} M`;
  }
  if (abs >= 1_000) {
    return `${signe}${Math.round(abs / 1_000).toLocaleString('fr-FR')} k`;
  }
  return `${signe}${Math.round(abs).toLocaleString('fr-FR')}`;
}

/** Montant complet pour les bulles d'aide et les phrases : « 1 234 567,89 MAD ». */
export function montantComplet(valeur: number, devise = 'MAD'): string {
  return `${valeur.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${devise}`;
}

/** Date ISO (yyyy-MM-dd) en « 20/11/2026 », sans passer par un fuseau horaire. */
export function dateCourte(iso: string): string {
  return `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}`;
}
