# G-Banque — Conventions Frontend

> Document de référence pour tout nouveau code frontend sur ce projet. Lu automatiquement par Claude au démarrage de chaque session.

Angular 17.3, composants standalone, PrimeNG 17.

## Style de code obligatoire (nouveau code)

1. **`inject()` partout** — jamais d'injection par constructeur, dans les services, guards ET composants.
2. **Reactive Forms** — jamais `ngModel`/template-driven forms. `FormBuilder`/`FormGroup`/`FormControl` avec validators.
3. **Nouveau control flow** — `@if`/`@for`/`@switch`, jamais `*ngIf`/`*ngFor`/`*ngSwitch`.
4. **Pas de Signals** — état géré via propriétés de classe classiques + RxJS (`BehaviorSubject`, etc.), cohérent avec `AuthService` existant. Choix délibéré de l'utilisateur, à revisiter plus tard si souhaité.
5. **Composants standalone** — déjà la convention du projet, à poursuivre.

## Gestion des erreurs — centralisée

Toute erreur HTTP (400/401/403/404/409/500, erreur réseau...) est interceptée une seule fois par `app-http-interceptor.interceptor.ts` et affichée via un toast PrimeNG (`MessageService` racine + `<p-toast>` global dans `AppComponent`), avec le message réel renvoyé par le backend (`ErrorResponse.message`).

**Conséquence pour tout nouveau code** : services et composants ne doivent **plus jamais** appeler `messageService.add({severity:'error', ...})` eux-mêmes — c'est le rôle exclusif de l'interceptor. Un composant peut toujours avoir un callback `error:` dans son `.subscribe()`, mais uniquement pour des effets de bord locaux (reset d'un flag `loading`, état de formulaire...), jamais pour afficher un message d'erreur.

Cas particuliers gérés dans l'interceptor :
- **401** (token invalide/expiré) : toast "session expirée" + déconnexion + redirection `/login`.
- **403** (autorisation refusée) : toast avec le message backend, **sans** déconnexion.
- **`/auth/login`** : toast avec le message backend, sans tentative de déconnexion (rien à déconnecter).

Les messages de **succès** (ex. "Utilisateur créé") restent gérés localement par chaque composant/service — l'interceptor ne s'occupe que des erreurs.

**Code existant** : ~123 fichiers ont aujourd'hui un callback `error:` qui appelle `messageService.add()` en doublon avec l'interceptor. Ce n'est pas encore nettoyé — chantier dédié séparé, traité module par module avec vérification (pas un remplacement global à l'aveugle, chaque callback peut avoir d'autres effets de bord à préserver).

## RBAC

- JWT : claim `authorities: string[]` (plus de `roles: string`). Authorities possibles : `SYSTEM_ADMIN` (transversal) et `DEPT_<nom>:<MEMBER|DIRECTOR>` (par département).
- `Role` enum (`USER`/`ADMIN`/`SUPER_ADMIN`) supprimé — n'existe plus côté backend.
- Guards : `systemAdminGuard` (remplace `adminGuard`), `departmentGuard(dept, minRole?)`.
- Sidebar : `SidebarItem.requiredAuthority?: string` (remplace `roles?: Role[]`).
