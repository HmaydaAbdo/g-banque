/**
 * Échappe les caractères HTML spéciaux d'une chaîne libre avant de l'interpoler
 * dans un `message` PrimeNG ConfirmDialog/Toast.
 *
 * <p>PrimeNG rend `confirmation.message` via `[innerHTML]`, pas via interpolation
 * Angular classique — un champ texte libre (nom de département, username...)
 * interpolé sans échappement est une faille XSS stockée potentielle (voir
 * Frontend/CLAUDE.md, section "Sécurité — XSS").
 */
export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
