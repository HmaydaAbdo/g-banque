/**
 * Détermine si l'utilisateur courant a l'autorité requise pour voir un élément de menu
 * (navbar ou sidebar).
 *
 * Logique RBAC partagée — factorisée le 16/07/2026 pour éliminer une duplication mot pour mot
 * entre `NavbarComponent.filterMenuItems()` et `SidebarComponent.hasRequiredAuthority()` (voir
 * `TODO.md`). Reste volontairement une simple fonction pure : seul le prédicat d'autorité était
 * dupliqué, pas la traversée (navbar filtre une liste plate, sidebar une arborescence récursive
 * avec élagage des parents sans enfant visible) — chaque composant garde donc sa propre logique de
 * parcours et appelle uniquement cette fonction pour la vérification d'autorité.
 *
 * @param requiredAuthority Autorité(s) requise(s) pour voir l'item. Absent → visible par tout
 *   utilisateur authentifié. Un tableau exprime un OR (ex. MEMBRE ou DIRECTEUR d'un même département).
 * @param isSystemAdmin Vrai si l'utilisateur courant est `SYSTEM_ADMIN` — accès transversal, voit
 *   toujours tout, quel que soit l'item.
 * @param authorities Autorités actuelles de l'utilisateur (départements/rôles).
 */
export function hasRequiredMenuAuthority(
  requiredAuthority: string | string[] | undefined,
  isSystemAdmin: boolean,
  authorities: string[]
): boolean {
  if (!requiredAuthority) return true;
  if (isSystemAdmin) return true;
  return Array.isArray(requiredAuthority)
    ? requiredAuthority.some(auth => authorities.includes(auth))
    : authorities.includes(requiredAuthority);
}
