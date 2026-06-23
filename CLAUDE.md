# G-Banque — Conventions Frontend

> Lu automatiquement par Claude au démarrage de chaque session.

Angular 17.3, composants standalone, PrimeNG 17.

## Style de code obligatoire (nouveau code)

1. `inject()` partout — jamais d'injection par constructeur (services, guards, composants).
2. Reactive Forms uniquement — jamais `ngModel`/template-driven forms.
3. Nouveau control flow `@if`/`@for`/`@switch` — jamais `*ngIf`/`*ngFor`/`*ngSwitch`.
4. Pas de Signals — état via propriétés de classe + RxJS (`BehaviorSubject`...), cohérent avec `AuthService`.
5. Composants standalone.

## Gestion des erreurs — centralisée

- Toute erreur HTTP est interceptée par `app-http-interceptor.interceptor.ts` → toast PrimeNG avec le message backend (`ErrorResponse.message`).
- Ne jamais appeler `messageService.add({severity:'error', ...})` dans un composant/service — rôle exclusif de l'interceptor.
- Un callback `error:` dans `.subscribe()` est autorisé uniquement pour des effets de bord locaux (`loading = false`, état de formulaire) — jamais pour afficher un message d'erreur.
- 401 → toast "session expirée" + déconnexion + redirection `/login`.
- 403 → toast avec message backend, sans déconnexion.
- `/auth/login` → toast avec message backend, sans tentative de déconnexion.
- Messages de succès gérés localement par chaque composant/service.

## Styles — toujours via les design tokens

- Tokens définis dans `src/styles/_tokens.scss` (`:root` + override `[data-theme="dark"]`).
- Aucune valeur brute (hex, `px`/`rem` en dur, `rgba(...)` ad hoc) si un token équivalent existe — toujours `var(--nom-token)`.
- Couleurs : `--color-*`, `--color-success/danger/warning/info/neutral` (+ `-hover`/`-light`/`-text`), `--status-*`.
- Espacement : `--space-1` à `--space-12`.
- Typographie : `--text-2xs` à `--text-3xl`, `--weight-*`, `--leading-*`, `--tracking-*`.
- Rayons/ombres/z-index : `--radius-*`, `--shadow-*`, `--z-*`.
- Réutiliser les classes utilitaires existantes (`.status-badge--en-cours|provisoire|definitif`, `.text-debit`/`.text-credit`) plutôt que d'en recréer.
- Si un token manque, l'ajouter dans `_tokens.scss` (avec override dark si pertinent) plutôt que de coder une valeur ponctuelle.

## Principes de conception

- **DRY** : réutiliser les tokens (`_tokens.scss`) et classes utilitaires partagées (`.status-badge`, `.text-debit`/`.text-credit`) — jamais redéfinir localement ce qui existe déjà.
- **SOLID** (adapté UI) :
  - SRP : un composant = un rôle (liste, formulaire, wizard step) ; extraire un service dès que la logique dépasse la présentation.
  - OCP : étendre un composant via `@Input()`/options plutôt que le dupliquer pour une variante.
  - DIP : un composant dépend de services injectés (`inject()`) — jamais d'appel HTTP direct depuis le composant.
- **DDD** : l'arborescence `portail/<domaine>` reflète les domaines métier du backend — jamais un domaine éclaté entre plusieurs dossiers.
- **KISS** : pas de state management custom ni de pattern réactif complexe si une propriété de classe + RxJS suffit (cf. "Pas de Signals").
- **YAGNI** : ne jamais ajouter un champ, une option ou un input non demandé dans un formulaire ou un composant.
- **Idempotence UI** : désactiver le bouton/action (`loading = true`) pendant un appel HTTP mutant pour empêcher une double soumission (double-clic).
- **Single Source of Truth** : `_tokens.scss` est l'unique source de vérité pour le style ; les enums/constantes partagés avec le backend ne doivent jamais diverger silencieusement.

## RBAC

- JWT : claim `authorities: string[]`. Authorities possibles : `SYSTEM_ADMIN` (transversal), `DEPT_<nom>:<MEMBER|DIRECTOR>` (par département).
- `Role` enum supprimé — n'existe plus côté backend.
- Guards : `systemAdminGuard`, `departmentGuard(dept, minRole?)`.
- Sidebar : `SidebarItem.requiredAuthority?: string | string[]` (un tableau exprime un OR — ex. `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']`).

## Sécurité — XSS

- Ne jamais interpoler un champ texte libre (saisi utilisateur) dans un `message` HTML de `ConfirmDialog`/`Toast` — PrimeNG le rend comme du HTML, pas du texte échappé. Un literal template `` `<strong>${champ}</strong>` `` avec un champ libre est une faille XSS stockée potentielle.
- Si une mise en forme (gras, icône...) est nécessaire dans une confirmation, structurer via un template Angular plutôt que construire la chaîne HTML à la main avec une valeur non garantie sûre.

## Lazy loading

- Toute nouvelle route utilise `loadComponent`/`loadChildren` — jamais d'import eager d'un composant de page dans `app.routes.ts`/`*.routes.ts`.
