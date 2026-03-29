/**
 * Utilitaires de gestion des dates — Timezone-safe
 *
 * Problème résolu : `new Date().toISOString()` convertit en UTC, ce qui entraîne
 * un décalage d'un jour (J-1) en timezone Maroc (UTC+1) et d'autres fuseaux positifs.
 *
 * Règle : toujours utiliser le temps LOCAL, jamais UTC, pour les dates métier.
 */

/**
 * Convertit un objet Date en chaîne `yyyy-MM-dd` en utilisant l'heure LOCALE.
 *
 * ❌ Ancienne approche : `date.toISOString().substring(0, 10)` → UTC → bug J-1
 * ✅ Cette approche : getFullYear/getMonth/getDate → heure locale → correct
 *
 * @param date La date à formater (peut être null/undefined)
 * @returns La chaîne au format `yyyy-MM-dd`, ou `null` si la date est absente
 */
export function toLocalDateString(date: Date | null | undefined): string | null {
  if (!date) return null;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Parse une chaîne `yyyy-MM-dd` (venant du backend) en Date LOCAL midnight.
 *
 * ❌ Ancienne approche : `new Date("2024-01-15")` → UTC midnight → affiché J-1 localement
 * ✅ Cette approche : `new Date(2024, 0, 15)` → minuit LOCAL → affiché correctement
 *
 * @param dateStr La chaîne au format `yyyy-MM-dd` (peut être null/undefined)
 * @returns Un objet Date à minuit heure locale, ou `null` si la chaîne est absente
 */
export function parseLocalDate(dateStr: string | null | undefined): Date | null {
  if (!dateStr) return null;
  const parts = dateStr.split('-');
  if (parts.length !== 3) return null;
  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1; // Les mois JS sont 0-indexés
  const day = parseInt(parts[2], 10);
  if (isNaN(year) || isNaN(month) || isNaN(day)) return null;
  return new Date(year, month, day);
}
