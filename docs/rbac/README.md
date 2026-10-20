# RBAC — G-Banque

> Suivi technique consolidé des accès (RBAC) de l'ensemble des modules G-Banque.
> Audience : développeurs, revue de sécurité, audit. Ce document ne contient ni logique
> métier, ni justification business, ni instructions destinées à l'utilisateur final —
> pour ces contenus, se reporter respectivement à `docs/modules/`, `docs/direction/` et
> `docs/guide-utilisateur/`.
>
> Ce fichier consolide les fiches individuelles historiquement présentes sous
> `docs/rbac/<module>.md`. Chaque fiche source reste la source de vérité pour son module ;
> ce README en est la vue agrégée.

## Sommaire

0. [Authentification — socle d'accès](#chap0)
1. [Comptes bancaires](#chap1)
2. [Crédit Moyen Terme](#chap2)
3. [Lignes Fonctionnelles](#chap3)
4. [Leasing](#chap4)
5. [Marchés](#chap5)
6. [Caution](#chap6)
7. [Décomptes](#chap7)
8. [Préfinancements](#chap8)
9. [Factoring](#chap9)
10. [Droits Constatés](#chap10)
11. [Chèque](#chap11)
12. [LCN](#chap12)
13. [Virement](#chap13)
14. [Virement International](#chap14)
15. [Mise à Disposition](#chap15)
16. [Recap Encaissement / Recap Décaissement](#chap16)
17. [Prélèvement](#chap17)
18. [Transactions](#chap18)
19. [Frais Bancaires](#chap19)
20. [Agios](#chap20)
21. [Impôts et Taxes](#chap21)
22. [Commission](#chap22)
23. [Impayés](#chap23)
24. [Opérations Erronées](#chap24)
25. [Provision](#chap25)
26. [Restitution](#chap26)
27. [Rapprochement](#chap27)
28. [Validation](#chap28)
29. [États des encours et Récapitulatif](#chap29)
30. [Logs](#chap30)
31. [Notifications](#chap31)
32. [Synchronisation FunPaiement](#chap32)

## Points ouverts

- Les modules transverses `department` et `user` n'ont pas encore de chapitre RBAC dédié
  (contrairement à `docs/modules/` et `docs/direction/`, qui en possèdent). Le module `auth`
  a le sien depuis le 10/10/2026 (chapitre 0).

---

<a id="chap0"></a>

## Chapitre 0 — Authentification (socle d'accès)

Sessions par cookies `HttpOnly` depuis le 10/10/2026 (aucun jeton dans `localStorage` ni dans le corps
des réponses). Jeton d'accès JWT de `JWT_ACCESS_TOKEN_MINUTES` (15) dans le cookie `GBANQUE_ACCESS`
(chemin `<context-path>`) ; jeton de rafraîchissement opaque, haché en base (`refresh_token`), dans le
cookie `GBANQUE_REFRESH` (chemin `<context-path>/auth`), session maximale `JWT_REFRESH_TOKEN_HOURS` (8).
Cookies `Secure`, `SameSite=Strict`.

### Endpoints (`SecurityController`, base `/auth`)

| Méthode | URL | Protection | CSRF |
|---|---|---|---|
| POST | `/auth/login` | `permitAll` (+ `LoginAttemptService`) | exempté (aucune session encore) |
| POST | `/auth/refresh` | `permitAll` — contrôle par le cookie `GBANQUE_REFRESH` (inconnu, expiré, révoqué ou réutilisé ⇒ 401 et cookies effacés ; compte désactivé ⇒ 401) | exigé |
| POST | `/auth/logout` | `permitAll` — révoque la session du cookie `GBANQUE_REFRESH` s'il existe, efface les cookies, toujours 204 | exigé |
| GET | `/auth/profile` | `@PreAuthorize("isAuthenticated()")` | — (lecture) |

### Règles transverses (`SecurityConfig`)

- **Lecture du jeton d'accès** (`CookieBearerTokenResolver`) : en-tête `Authorization: Bearer` s'il est
  présent, sinon cookie `GBANQUE_ACCESS`. Aucun jeton n'est lu sur `/auth/login`, `/auth/refresh`,
  `/auth/logout` (un jeton expiré ne doit pas les faire échouer).
- **CSRF** : jeton double-submit, cookie `XSRF-TOKEN` (non `HttpOnly`, chemin `/`) recopié par Angular
  dans l'en-tête `X-XSRF-TOKEN` ; exigé sur toute requête modifiante (POST, PUT, PATCH, DELETE) de tous
  les modules. Exemptions : `/auth/login`, et toute requête portant un en-tête `Authorization` (pas
  d'authentification par cookie, donc pas de CSRF possible).
- **Divergence assumée** : l'en-tête `Authorization: Bearer` reste accepté en secours (Swagger, scripts,
  outils) ; le navigateur n'utilise que les cookies. Justification : garder Swagger utilisable en dev.
- Les droits (`authorities`) sont relus en base à chaque rafraîchissement : effet d'un changement de
  rôle ou d'une désactivation sous 15 minutes.

### Frontend

- Aucun guard sur `/login` (route publique). Les guards `authGuard`, `accueilDispatchGuard`,
  `systemAdminGuard`, `departmentGuard(...)` vérifient `AuthService.isAuthenticated()` (profil de session
  chargé au démarrage via `GET /auth/profile`, `APP_INITIALIZER`) avant tout contrôle d'autorité.
- Les droits affichés (`isSystemAdmin()`, `belongsToDepartment()`, `hasDepartmentRole()`) proviennent
  du profil renvoyé par le backend, plus du décodage local d'un jeton.
- Intercepteur : sur 401, un seul `POST /auth/refresh` partagé puis la requête est rejouée une fois ;
  échec ⇒ message « Session expirée » et retour à `/login`.
- Bouton de déconnexion (navbar) visible pour tout utilisateur connecté ; appelle `POST /auth/logout`.

---

<a id="chap1"></a>
## Chapitre 1 — Comptes bancaires

---

### Endpoints (`BanqueController`, base `/banques`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/banques` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/banques/{id}` | `isAuthenticated()` | Donnée de référence consommée par d'autres modules indépendants (dropdowns : Agios, Commissions, Droits constatés, Leasing...). |
| GET | `/banques` | `isAuthenticated()` | Idem — liste de référence, pas de donnée sensible exposée (le logo et le solde ne sont pas filtrés par rôle). |
| GET | `/banques/accueil-info` | `FINANCE_OR_ADMIN` | Tableau de bord Finance — solde, plafond FC, échus, projection jusqu'à `?periode=FIN_DE_MOIS` (défaut) \| `FIN_DE_TRIMESTRE` (paramètre de lecture, sans effet sur les droits). Réservé Finance/admin. |
| GET | `/banques/accueil-info/{banqueId}` | `FINANCE_OR_ADMIN` | Idem, restreint à une banque (même paramètre `periode`). |
| PUT | `/banques/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/banques/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |

RBAC posé par méthode (pas au niveau classe) — restrictions non uniformes entre lecture et écriture. Voir le Javadoc de `BanqueController`.

---

### Guards Angular (`comptes-bancaire-routing.module.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/comptes-bancaires` (parent — s'applique à tous les enfants : liste, `new`, `edit/:id`, `history/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours (accès transversal). |

**Divergence assumée backend / frontend :** l'API `GET /banques` et `GET /banques/{id}` reste ouverte à tout authentifié côté backend, alors que la route Angular `/portail/comptes-bancaires` est réservée au département Finance. Justification (voir commentaire dans `comptes-bancaire-routing.module.ts`) : l'écran de gestion des comptes est Finance-only, mais l'API sert de donnée de référence à d'autres modules métier (listes déroulantes) qui ne doivent pas être cassés pour les autres départements.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Comptes Bancaires" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (liste des comptes) | `isSystemAdmin()` | `ComptesBancairesComponent` |
| Champ "Numéro de compte" désactivé en modification pour un non-admin | `isEditMode && !authService.isSystemAdmin()` | `BanqueEditComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter la liste / le détail d'un compte (API) | Tout collaborateur authentifié |
| Accéder à l'écran "Comptes bancaires" (Angular) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Consulter le tableau de bord Finance (`accueil-info`) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Créer / modifier un compte | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Supprimer un compte | `SYSTEM_ADMIN` uniquement |

*Les comptes bancaires servent de support aux produits financiers suivants : CMT, Lignes fonctionnelles et Leasing.*

<a id="chap2"></a>
## Chapitre 2 — Crédit Moyen Terme

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `CreditMoyenTermeController` (base `/credits-cmt`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/credits-cmt` | `FINANCE_OR_ADMIN` | Création d'un crédit CMT. |
| GET | `/credits-cmt/actifs` | `FINANCE_OR_ADMIN` | Liste des crédits encours. |
| GET | `/credits-cmt/en-attente-de-main-levees` | `FINANCE_OR_ADMIN` | Liste des crédits en attente de main levée. |
| GET | `/credits-cmt/archives` | `FINANCE_OR_ADMIN` | Liste des crédits archivés. |
| PUT | `/credits-cmt/{id}/archiver` | `FINANCE_OR_ADMIN` | Clôture (main levée) — bloquée si échéances encore `PREVU`. |
| GET | `/credits-cmt/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| PUT | `/credits-cmt/{id}` | `FINANCE_OR_ADMIN` | Modification — régénère les échéances. |
| GET | `/credits-cmt/actifs/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — nouveau. Mêmes critères que `/credits-cmt/actifs`, sans pagination (tous les résultats filtrés). |
| GET | `/credits-cmt/en-attente-de-main-levees/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — nouveau. Mêmes critères que `/credits-cmt/en-attente-de-main-levees`, sans pagination. |
| GET | `/credits-cmt/archives/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — nouveau. Mêmes critères que `/credits-cmt/archives`, sans pagination. |
| DELETE | `/credits-cmt/{id}` | `ADMIN_ONLY` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Bloquée en service si des échéances sont déjà payées. |

> **Nettoyé le 2026-07-21** : l'endpoint `GET /credits-cmt/{creditId}/tableau-amortissement` a été supprimé — c'était un doublon exact de `GET /echeances-cmt/tableau-amortissement/{creditId}` (même service, même résultat), non consommé par le frontend (vérifié par grep), et constituait du dead code au sens de `CLAUDE.md`.

---

### Endpoints — `EcheanceCMTController` (base `/echeances-cmt`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/echeances-cmt/tableau-amortissement/{creditId}` | `FINANCE_OR_ADMIN` | Tableau d'amortissement — seul point d'entrée désormais (l'ancien doublon sous `/credits-cmt/` a été supprimé, cf. note ci-dessus). Consommé par `TableauAmortissementComponent`. |
| GET | `/echeances-cmt/tableau-amortissement/{creditId}/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — nouveau. Échéancier complet du crédit, mêmes critères que l'endpoint ci-dessus, sans pagination. |
| PUT | `/echeances-cmt/{id}/toggle-status` | `ADMIN_ONLY` | Bascule PREVU ↔ PAYE — action sensible réservée à l'admin système. |
| PUT | `/echeances-cmt/{id}` | `ADMIN_ONLY` | Modification d'une échéance `PREVU` (date, montant, dates d'opération/valeur, observation, libellé). Bloqué si `PAYE`. |
| GET | `/echeances-cmt/encours` | `FINANCE_OR_ADMIN` | Encours de CMT par banque, filtrable par désignation/montant. |
| GET | `/echeances-cmt/encours/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours CMT d'une banque, mêmes critères que l'endpoint ci-dessus, sans pagination. Écran "Décaissements / Échéances CMT" du module États des encours. |

---

### Endpoints — `CmtBatchController` (base `/batch/cmt`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/batch/cmt/run` | `hasAuthority('SYSTEM_ADMIN')` | Déclenchement manuel du job Main Levée — réservé admin système. |
| GET | `/batch/cmt/status` | `hasAuthority('SYSTEM_ADMIN')` | Consultation du statut du job — réservé admin système. |

> **Renommé le 2026-07-04** : préfixe `/test/` retiré (`/test/batch/cmt` → `/batch/cmt`), mirroring le renommage déjà effectué sur `LeasingBatchController` — aucune référence frontend à l'ancienne URL (vérifié par grep sur tout `Frontend/src`).

---

### Endpoints — `CmtConfigController` (base `/cmt-config`) — nouveau

| Méthode | URL | `@PreAuthorize` (classe) | Justification |
|---|---|---|---|
| GET | `/cmt-config` | `hasAuthority('SYSTEM_ADMIN')` | Lecture de la configuration (seuils d'alerte échéances à venir). |
| PUT | `/cmt-config` | `hasAuthority('SYSTEM_ADMIN')` | Modification — mêmes règles d'accès que `/leasing-config`/`/commission-config` (paramètres métier globaux). |

Créé le 2026-07-04 pour remplacer les seuils J-7/J-30 auparavant codés en dur dans `CmtMetrics` (violation `conventions/configuration.md`), mirroring exactement le pattern `LeasingConfig`.

---

### Endpoint — `ValidationController` (base `/rapprochement`, hors périmètre `cmt/`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/rapprochement/process` | `FINANCE_OR_ADMIN` (hérité du niveau classe) | Validation d'une échéance CMT (parmi toutes les autres sources) — crée la transaction bancaire et passe l'échéance en PAYE. Dispatche vers `CmtBatchValidationHandler`, qui délègue à `ICmtValidationService.processCmtTransaction()` (module `cmt`, cf. Bounded Context — `architecture.md`, Règle 1). |

Cet endpoint appartient au controller `ValidationController` du module `validation`, protégé au niveau classe par `@PreAuthorize(FINANCE_OR_ADMIN)` (sécurisé lors du sweep Leasing, cf. `docs/rbac/leasing.md`, puis consolidé le 19/09/2026 — voir Chapitre 28 — Validation). Il n'est donc pas re-documenté en détail ici — seule sa présence et sa délégation vers le module `cmt` sont trackées pour la cohérence de ce document.

---

### Guards Angular (`cmt-routing.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/cmt` (parent — `encours`, `en-attente-de-ML`, `archives`, `new`, `edit/:id`, `:id/tableau-amortissement`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/cmt/echeances/edit/:id` — nouveau (21/09/2026) | `departmentGuard('Finance')` (hérité du parent) + `systemAdminGuard` (posé sur la route elle-même) | `SYSTEM_ADMIN` uniquement — remplace le dialogue d'édition d'échéance de `TableauAmortissementComponent`, même restriction que le `PUT /echeances-cmt/{id}` (`ADMIN_ONLY`) qu'elle appelle. Atteignable aussi depuis `RecapitulatifComponent` (`CREATE_EDIT_ELIGIBILITY[CMT]`), qui vérifie `authService.isSystemAdmin()` avant d'afficher le bouton « Éditer ». |
| `/portail/admin/cmt-config` (parent `admin.routes.ts`) — nouveau | `systemAdminGuard` (posé au niveau de `/portail/admin` dans `portail.routes.ts`) | `SYSTEM_ADMIN` uniquement. |

> **Corrigé pendant ce sweep** : la route racine `cmt-routing.ts` n'avait initialement **aucun guard** — accessible à tout utilisateur authentifié quel que soit son département. Ajout de `departmentGuard('Finance')` sur toutes les routes du module, cohérent avec le backend et avec le pattern déjà en place sur `leasing-routing.module.ts`.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Crédits Moyen Terme" (section) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |
| "Les Crédits encours" | idem |
| "Les Crédits en attente de main levées" | idem |
| "Les Crédits archivés" | idem |
| "Configuration CMT" (sous "Administration") — nouveau | `SYSTEM_ADMIN` (hérité de la section "Administration") |

> **Corrigé pendant ce sweep** : la section et ses trois sous-items n'avaient initialement **aucun `requiredAuthority`** — visibles dans le menu pour tout utilisateur connecté, alors que la route elle-même est protégée par `departmentGuard('Finance')`. Ajout de `requiredAuthority` sur la section et chaque sous-item, cohérent avec le pattern déjà en place sur la section "Leasing".

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (crédit CMT) | `isSystemAdmin()` | `CmtListComponent` (vue "encours") |
| Colonne "Actions" + toggle statut échéance (PREVU ↔ PAYE) | `isAdmin()` | `TableauAmortissementComponent` |
| Bouton crayon "Modifier l'échéance" (visible uniquement si `statutPaiement === 'PREVU'`) — nouveau | `isAdmin()` | `TableauAmortissementComponent` |

> **Corrigé pendant ce sweep** : `TableauAmortissementComponent` exposait le toggle de statut à tout utilisateur Finance via `[ngModel]` non conditionné, alors que le backend `PUT /echeances-cmt/{id}/toggle-status` est `ADMIN_ONLY`. La colonne "Actions" et la cellule de toggle sont désormais encadrées par `@if (isAdmin())`, et le binding a été corrigé pour exprimer un booléen (`echeance.statutPaiement === 'PAYE'`) au lieu de transmettre l'enum brut — alignement exact sur le pattern déjà en place dans `TableauAmortissementComponent` (Leasing).

**Pas de divergence assumée backend/frontend** sur ce module, une fois les deux corrections ci-dessus appliquées : les guards de route, la visibilité sidebar et les conditions d'affichage reflètent exactement les `@PreAuthorize` backend.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un crédit CMT, consulter / valider une échéance | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Clôturer un crédit CMT (main levée) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Supprimer un crédit CMT | `SYSTEM_ADMIN` uniquement |
| Basculer manuellement le statut d'une échéance | `SYSTEM_ADMIN` uniquement |
| Modifier manuellement une échéance PRÉVUE | `SYSTEM_ADMIN` uniquement |
| Déclencher / consulter manuellement le batch Main Levée | `SYSTEM_ADMIN` uniquement |
| Consulter / modifier la configuration CMT (seuils d'alerte) | `SYSTEM_ADMIN` uniquement |
| Exporter en PDF une liste de crédits ou un tableau d'amortissement — nouveau | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — même niveau d'accès que la consultation de l'écran correspondant, aucune restriction supplémentaire dans l'UI |

<a id="chap3"></a>
## Chapitre 3 — Lignes Fonctionnelles

Le module regroupe trois sous-domaines : **Nature** (référentiel), **Générale** et **Spécifique**
(les deux types de lignes fonctionnelles elles-mêmes), plus un **batch** de synchronisation.

**État d'avancement du sweep de conformité (2026-07-06)** : le module `LigneFonctionnelle` est
désormais audité intégralement — backend (Nature, Générale, Spécifique, batch) et frontend (Nature,
Générale, Spécifique) — voir `modules_sweep` en mémoire pour le détail complet et les points
reportés (dette Bounded Context, IDs magiques, DTOs non-record).

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN  = "hasAuthority('SYSTEM_ADMIN')";
private static final String AUTHENTICATED = "isAuthenticated()";
```

---

### Endpoints — `NatureLigneFonctionnelleController` (base `/natures-lignes-fonctionnelles`) ✅ audité

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/natures-lignes-fonctionnelles` | `FINANCE_OR_ADMIN` | Création d'une nature — référentiel administré par Finance. |
| GET | `/natures-lignes-fonctionnelles/{id}` | `AUTHENTICATED` | Référentiel consulté par tout utilisateur authentifié (dropdowns Générale/Spécifique). |
| GET | `/natures-lignes-fonctionnelles` | `AUTHENTICATED` | Liste paginée — idem. |
| GET | `/natures-lignes-fonctionnelles/list` | `AUTHENTICATED` | Liste non paginée pour dropdowns — idem. |
| PUT | `/natures-lignes-fonctionnelles/{id}` | `FINANCE_OR_ADMIN` | Modification (label/description/portée ; le code est immuable). |
| DELETE | `/natures-lignes-fonctionnelles/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Bloquée si la nature est référencée par une ligne fonctionnelle (générale ou spécifique). |
| GET | `/natures-lignes-fonctionnelles/export/pdf` | `AUTHENTICATED` | Export PDF (mêmes critères que la liste, sans pagination) — même règle d'accès que la lecture, référentiel inter-modules. |

> **Corrigé pendant ce sweep (2026-07-04)** : le controller n'avait initialement **aucun**
> `@PreAuthorize`, `@Tag`, `@Operation` ni `@SecurityRequirement` — tous les endpoints étaient
> accessibles à quiconque possédait un token valide, y compris la création/modification/suppression
> du référentiel. Ajout complet des annotations Swagger et RBAC, mirroring `BanqueController`
> (lecture `AUTHENTICATED` car donnée de référence inter-modules, écriture `FINANCE_OR_ADMIN`,
> suppression `SYSTEM_ADMIN`).

---

### Endpoints — `LigneFonctionnelleController` (base `/lignes-fonctionnelles`) ✅ audité

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/lignes-fonctionnelles` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| PUT | `/lignes-fonctionnelles/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| GET | `/lignes-fonctionnelles/{id}` | `FINANCE_OR_ADMIN` | Détail — donnée financière, pas un référentiel dropdown (contrairement à Nature). |
| GET | `/lignes-fonctionnelles` | `FINANCE_OR_ADMIN` | Liste paginée. |
| GET | `/lignes-fonctionnelles/marche/{marcheId}` | `FINANCE_OR_ADMIN` | Lignes d'un marché. |
| PUT | `/lignes-fonctionnelles/deactivate/{ligneId}` | `FINANCE_OR_ADMIN` | Désactivation. |
| DELETE | `/lignes-fonctionnelles/{id}` | `SYSTEM_ADMIN` | Règle transverse (`security.md`) : suppression toujours `SYSTEM_ADMIN`. Interdit directement sur une sous-ligne. |
| PUT | `/lignes-fonctionnelles/{ligneMereId}/sous-lignes` | `FINANCE_OR_ADMIN` | Gestion des sous-lignes d'une ligne mère. |
| GET | `/lignes-fonctionnelles/banque/{banqueId}` | `FINANCE_OR_ADMIN` | Lignes d'une banque. |
| GET | `/lignes-fonctionnelles/{id}/prefinancements` | `FINANCE_OR_ADMIN` | Suivi préfinancements. |
| GET | `/lignes-fonctionnelles/{id}/dc` | `FINANCE_OR_ADMIN` | Suivi droits constatés. |
| GET | `/lignes-fonctionnelles/{id}/factoring` | `FINANCE_OR_ADMIN` | Suivi factoring. |
| GET | `/lignes-fonctionnelles/{id}/lcn` | `FINANCE_OR_ADMIN` | Suivi LCN. |
| GET | `/lignes-fonctionnelles/history` | `FINANCE_OR_ADMIN` | Historique taux/plafond pour graphique. |
| GET | `/lignes-fonctionnelles/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF (mêmes critères que la liste, sans pagination) — réutilisé par les écrans encours (`actif=true`) et archivées (`actif=false`). |

> **Corrigé pendant ce sweep (2026-07-06)** : le controller n'avait initialement **aucun**
> `@PreAuthorize`, `@Tag`, `@Operation` ni `@SecurityRequirement` — tous les endpoints (14) étaient
> accessibles à quiconque possédait un token valide. Choix `FINANCE_OR_ADMIN` pour toutes les
> lectures (et pas `AUTHENTICATED` comme Nature) car il s'agit de données financières propres au
> département Finance (plafonds, taux, encours), pas d'un référentiel dropdown consommé par
> d'autres départements — mirroring `CreditMoyenTermeController`/`ContratLeasingController`.
> Injection également corrigée : le controller dépendait de la classe concrète
> `LigneFonctionnelleService` (violation DIP), remplacée par la nouvelle interface complète
> `ILigneFonctionnelleService` (17 méthodes).

---

### Endpoints — `LigneFonctionnelleSpecifiqueController` (base `/lignes-fonctionnelles-specifiques`) ✅ audité

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/lignes-fonctionnelles-specifiques` | `FINANCE_OR_ADMIN` | Création — donnée financière Finance. |
| GET | `/lignes-fonctionnelles-specifiques/{id}` | `FINANCE_OR_ADMIN` | Détail. |
| GET | `/lignes-fonctionnelles-specifiques` | `FINANCE_OR_ADMIN` | Liste paginée. |
| PUT | `/lignes-fonctionnelles-specifiques/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| GET | `/lignes-fonctionnelles-specifiques/banque/{banqueId}` | `FINANCE_OR_ADMIN` | Lignes d'une banque. |
| PUT | `/lignes-fonctionnelles-specifiques/deactivate/{ligneId}` | `FINANCE_OR_ADMIN` | Désactivation. |
| DELETE | `/lignes-fonctionnelles-specifiques/{id}` | `SYSTEM_ADMIN` | Règle transverse (`security.md`). |
| GET | `/lignes-fonctionnelles-specifiques/marche/{marcheId}` | `FINANCE_OR_ADMIN` | Lignes d'un marché. |
| GET | `/lignes-fonctionnelles-specifiques/{id}/prefinancements` | `FINANCE_OR_ADMIN` | Suivi préfinancements. |
| GET | `/lignes-fonctionnelles-specifiques/{id}/dc` | `FINANCE_OR_ADMIN` | Suivi droits constatés. |
| GET | `/lignes-fonctionnelles-specifiques/{id}/factoring` | `FINANCE_OR_ADMIN` | Suivi factoring. |
| GET | `/lignes-fonctionnelles-specifiques/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF (mêmes critères que la liste, sans pagination) — réutilisé par les écrans encours (`actif=true`) et archivées (`actif=false`). |

> **Corrigé pendant ce sweep (2026-07-06)** : même constat initial que la générale (aucun
> `@PreAuthorize`/`@Tag`/Swagger, injection de la classe concrète). Corrigé de manière identique
> (`FINANCE_OR_ADMIN` lectures/écritures, `SYSTEM_ADMIN` suppression, injection de la nouvelle
> interface complète `ILigneFonctionnelleSpecifiqueService`, 12 méthodes). Entité
> `LigneFonctionnelleSpecifique` passée en `@Audited` + index (`nature_ligne_id`, `marche_id`,
> `actif`) ; relation vers `Marche` (non auditée) marquée `NOT_AUDITED`.

---

### Endpoints — `FunctionalLinesBatchController` (base `/batch/functional-lines`) ✅ audité

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/batch/functional-lines/run-general` | `SYSTEM_ADMIN` | Déclenchement manuel — réservé admin, mirroring `CmtBatchController`/`LeasingBatchController`. |
| POST | `/batch/functional-lines/run-specific` | `SYSTEM_ADMIN` | idem. |
| POST | `/batch/functional-lines/run-all` | `SYSTEM_ADMIN` | idem. |
| GET | `/batch/functional-lines/status` | `SYSTEM_ADMIN` | Consultation du statut des jobs. |

> **Corrigé pendant ce sweep (2026-07-06)** : préfixe `/test/` retiré (renommé en `/batch/`,
> décision utilisateur déjà validée — aucune référence frontend à l'ancienne URL, vérifié par grep).
> Aucun `@PreAuthorize`/`@Tag`/`@SecurityRequirement` initialement — tous les endpoints étaient
> accessibles à quiconque possédait un token valide, y compris le déclenchement manuel des jobs.
> Ajout de `SYSTEM_ADMIN` sur tous les endpoints (déclenchement de batch = opération sensible,
> mirroring `CmtBatchController`) + endpoint `/status` ajouté (absent initialement, présent côté CMT)
> pour exposer `isSchedulerEnabled`/`getJobsCron`/`isGeneralJobRunning`/etc., déjà disponibles sur
> `FunctionalLinesJobScheduler` mais non exposés via API. Logging retiré du controller (interdit,
> `logging.md` — réservé aux Services ; `FunctionalLinesJobScheduler` conserve son logging détaillé,
> légitime pour un composant d'orchestration batch).

---

### Endpoints — `LigneFonctionnelleConfigController` (base `/lignes-fonctionnelles-config`) — nouveau

| Méthode | URL | `@PreAuthorize` (classe) | Justification |
|---|---|---|---|
| GET | `/lignes-fonctionnelles-config` | `hasAuthority('SYSTEM_ADMIN')` | Lecture de la configuration (seuils d'alerte/avertissement d'expiration). |
| PUT | `/lignes-fonctionnelles-config` | `hasAuthority('SYSTEM_ADMIN')` | Modification — mêmes règles d'accès que `/cmt-config`/`/leasing-config`/`/commission-config` (paramètres métier globaux). |

Créé le 2026-07-06 pour remplacer les propriétés `@Value` (`functional.line.expiration.alert.threshold.months`
et `.warning.threshold.months`) utilisées par `FunctionalLineProcessor` et `SpecificFunctionalLineProcessor` —
ces propriétés n'avaient d'ailleurs aucune valeur définie dans `application.yml`/`-dev`/`-prod` (trou de
configuration comblé par cette entité). Mirroring exact du pattern `CmtConfig`/`LeasingConfig` : entité
singleton (`id = 1`), amorçage `@PostConstruct` (valeurs historiques 1 mois / 3 mois), façade
`ILigneFonctionnelleConfigService` injectée par les deux processors (jamais le Repository directement).

---

### Guards Angular (`natures-routing.module.ts`) ✅ audité

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/natures-fonctionnelles` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |

> **Corrigé pendant ce sweep** : la route n'avait initialement **aucun guard**. Ajout de
> `departmentGuard('Finance')`, cohérent avec `cmt-routing.ts`/`leasing-routing.module.ts`. Routes
> également passées en Lazy Loading (`loadComponent`), elles utilisaient auparavant `component:`
> (import statique).

### Guards Angular (`lignes-fonctionnelles.routes.ts`) ✅ audité (Générale)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/lignes-fonctionnelles-encours` (liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/lignes-fonctionnelles-archived` | `departmentGuard('Finance')` | idem. |

> **Corrigé pendant ce sweep (2026-07-06)** : aucun guard initialement (même gap que Nature avant
> son sweep) — ajout de `departmentGuard('Finance')`, mirroring `cmt-routing.ts`.

### Guards Angular (`lignes-specifiques.routes.ts`) ✅ audité (Spécifique)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/lignes-fonctionnelles-specifiques-encours` (liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/lignes-fonctionnelles-specifiques-archived` | `departmentGuard('Finance')` | idem. |

> **Corrigé pendant ce sweep (2026-07-06)** : aucun guard initialement (même gap que Nature/Générale
> avant leur sweep) — ajout de `departmentGuard('Finance')`, mirroring `cmt-routing.ts`.

### Guards Angular (`admin.routes.ts`) — nouveau

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/admin/lignes-fonctionnelles-config` (parent `admin.routes.ts`) | `systemAdminGuard` (posé au niveau de `/portail/admin` dans `portail.routes.ts`) | `SYSTEM_ADMIN` uniquement. |

---

### Visibilité sidebar (`sidebar.component.ts`) ✅ corrigé pour l'ensemble de la section

| Item | `requiredAuthority` |
|---|---|
| "Les Lignes Fonctionnelles" (section) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |
| "Les Lignes ponctuelles" (sous-section Générale) + ses 2 items | idem |
| "Les Lignes spécifiques" (sous-section) + ses 2 items | idem |
| "Les Natures des lignes Fonctionnelles" | idem |
| "Configuration Lignes Fonctionnelles" (sous "Administration") — nouveau | `SYSTEM_ADMIN` (hérité de la section "Administration") |

> **Corrigé pendant ce sweep** : toute la section "Les Lignes Fonctionnelles" (7 items au total,
> couvrant Nature/Générale/Spécifique) n'avait **aucun `requiredAuthority`** — visible pour tout
> utilisateur connecté quel que soit son département. Corrigé en une seule fois pour l'ensemble de
> la section (même fichier, même pattern que "Les Crédits Moyen Terme"), même si Générale/Spécifique
> ne sont pas encore auditées côté backend — cette correction sidebar ne dépend pas du sweep code de
> ces sous-modules et ferme un gap de sécurité visible immédiatement.

---

### Actions conditionnées dans l'UI ✅ audité pour Nature

| Action | Condition d'affichage | Composant |
|---|---|---|
| Boutons "Modifier" / "Supprimer" (nature) | `isAdmin()` (`isSystemAdmin()`) | `NatureLigneFonctionnelleListComponent` |

**Divergence backend/frontend assumée** : le bouton "Modifier" est masqué aux utilisateurs
non-admin dans `NatureLigneFonctionnelleListComponent`, alors que le backend autorise
`FINANCE_OR_ADMIN` pour `PUT /natures-lignes-fonctionnelles/{id}`. C'est une restriction frontend
plus stricte que le backend (pas une faille) : un utilisateur Finance non-admin ne voit pas le
bouton mais serait autorisé côté API. Non corrigé dans ce sweep (périmétre : audit de conformité,
pas de changement de comportement fonctionnel) — signalé ici pour transparence, à trancher par
l'utilisateur si un écart est indésirable.

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter le référentiel des natures | Tout utilisateur authentifié | ✅ conforme |
| Créer / modifier une nature | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme |
| Supprimer une nature | `SYSTEM_ADMIN` uniquement, bloqué si référencée | ✅ conforme |
| Exporter en PDF les natures | Tout utilisateur authentifié | ✅ conforme |
| Consulter / créer / modifier / supprimer une ligne fonctionnelle générale | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` ; suppression `SYSTEM_ADMIN` uniquement | ✅ conforme |
| Exporter en PDF les lignes fonctionnelles générales (encours/archivées) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme |
| Consulter / créer / modifier / supprimer une ligne fonctionnelle spécifique | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` ; suppression `SYSTEM_ADMIN` uniquement | ✅ conforme |
| Exporter en PDF les lignes fonctionnelles spécifiques (encours/archivées) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme |
| Déclencher le batch de synchronisation | `SYSTEM_ADMIN` uniquement | ✅ conforme |
| Consulter / modifier la configuration Lignes Fonctionnelles (seuils d'alerte/avertissement) | `SYSTEM_ADMIN` uniquement | ✅ conforme |

<a id="chap4"></a>
## Chapitre 4 — Leasing

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `OrganismeLeasingController` (base `/OrganismesLeasing`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/OrganismesLeasing` | `FINANCE_OR_ADMIN` | Création réservée à l'équipe Finance ou l'admin système. |
| PUT | `/OrganismesLeasing/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou l'admin système. |
| GET | `/OrganismesLeasing/{id}` | `FINANCE_OR_ADMIN` | Lecture réservée au module (pas de donnée transverse comme Bank). |
| DELETE | `/OrganismesLeasing/{id}` | `ADMIN_ONLY` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Bloquée en service si des contrats cadres sont liés. |
| GET | `/OrganismesLeasing` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée. |
| GET | `/OrganismesLeasing/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — mêmes critères que la recherche, sans pagination. |

---

### Endpoints — `ContratCadreController` (base `/contrats-cadres`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/contrats-cadres` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée, filtrable par organisme/statut. |
| POST | `/contrats-cadres` | `FINANCE_OR_ADMIN` | Création — bloquée si l'organisme a déjà un contrat cadre `ACTIF`. |
| PUT | `/contrats-cadres/{id}` | `FINANCE_OR_ADMIN` | Modification du plafond et des dates (organisme non modifiable). |
| GET | `/contrats-cadres/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/contrats-cadres/organisme/{organismeId}` | `FINANCE_OR_ADMIN` | Historique des contrats cadres d'un organisme. |
| PATCH | `/contrats-cadres/{id}/archiver` | `FINANCE_OR_ADMIN` | Passage `ACTIF` → `ARCHIVE`, irréversible. |
| GET | `/contrats-cadres/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — mêmes critères que la recherche, sans pagination. |

**Pas d'endpoint DELETE** : un contrat cadre n'est jamais supprimé, uniquement archivé (voir `docs/modules/leasing.md`). Cohérent avec la règle "suppression = `SYSTEM_ADMIN`" qui ne s'applique donc pas ici — il n'y a pas de suppression possible du tout.

---

### Endpoints — `ContratLeasingController` (base `/contratsLeasing`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/contratsLeasing` | `FINANCE_OR_ADMIN` | Création d'un contrat de leasing. |
| GET | `/contratsLeasing/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| PUT | `/contratsLeasing/{id}` | `FINANCE_OR_ADMIN` | Modification — régénère les échéances. |
| PUT | `/contratsLeasing/{id}/markAsInactive` | `FINANCE_OR_ADMIN` | Clôture (main levée) — bloquée si échéances encore `PREVU`. |
| DELETE | `/contratsLeasing/{id}` | `ADMIN_ONLY` | Règle transverse suppression. Bloquée en service si échéances déjà payées. |
| GET | `/contratsLeasing/actifs` | `FINANCE_OR_ADMIN` | Liste des contrats encours. |
| GET | `/contratsLeasing/en-attente-main-levee` | `FINANCE_OR_ADMIN` | Liste des contrats en attente de main levée. |
| GET | `/contratsLeasing/inactifs` | `FINANCE_OR_ADMIN` | Liste des contrats clôturés. |
| GET | `/contratsLeasing/actifs/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF des contrats encours — mêmes critères que `/actifs`, sans pagination. |
| GET | `/contratsLeasing/en-attente-main-levee/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF des contrats en attente de main levée — mêmes critères que `/en-attente-main-levee`, sans pagination. |
| GET | `/contratsLeasing/inactifs/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF des contrats clôturés — mêmes critères que `/inactifs`, sans pagination. |

---

### Endpoints — `EcheanceLeasingController` (base `/echeances-leasing`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/echeances-leasing/tableau-amortissement/{contratId}` | `FINANCE_OR_ADMIN` | Tableau d'amortissement complet d'un contrat (seul point d'entrée — voir note ci-dessous). |
| PUT | `/echeances-leasing/{id}/toggle-status` | `ADMIN_ONLY` | Bascule PREVU ↔ PAYE — action sensible réservée à l'admin système. |
| PUT | `/echeances-leasing/{id}` | `ADMIN_ONLY` | Modification d'une échéance `PREVU` (montant, dates, libellé). Bloqué si `PAYE`. |
| GET | `/echeances-leasing/avance/{contratId}` | `FINANCE_OR_ADMIN` | Avance (échéance n°1) d'un contrat et son moyen de paiement (nouveau 19/10/2026). Refusé si le contrat n'a pas d'avance. |
| GET | `/echeances-leasing/avance/moyens-disponibles?type=&recherche=&contratId=&page=&size=` | `FINANCE_OR_ADMIN` | Décaissements non annulés (réglés ou en cours) d'un type, hors ceux déjà rattachés à l'avance d'un autre contrat, par pages (liste déroulante du wizard). Nouveau 20/10/2026. |
| GET | `/echeances-leasing/encours` | `FINANCE_OR_ADMIN` | Encours de leasing par banque, filtrable par organisme/période/montant. |
| GET | `/echeances-leasing/tableau-amortissement/{contratId}/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF du tableau d'amortissement complet — mêmes critères que la consultation, sans pagination, avec les totaux Endettement/Payé/Restant. |
| GET | `/echeances-leasing/encours/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours de leasing d'une banque, mêmes critères que `/echeances-leasing/encours`, sans pagination. Écran "Décaissements / Échéances Leasing" du module États des encours. |

**Nettoyage effectué pendant ce sweep** : `ContratLeasingController` exposait un second endpoint dupliqué (`GET /contratsLeasing/{contratId}/tableauAmortissement`) jamais appelé par le frontend — supprimé (code mort). `EcheanceLeasingController.getTableauAmortissement` reste le seul point d'entrée.

---

### Endpoints — `LeasingBatchController` (base `/batch/leasing`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/batch/leasing/run` | `hasAuthority('SYSTEM_ADMIN')` | Déclenchement manuel du job Main Levée — réservé admin système. |

> **Renommé le 2026-07-04** : préfixe `/test/` retiré (`/test/batch/leasing` → `/batch/leasing`) — aucune référence frontend à l'ancienne URL (vérifié par grep sur tout `Frontend/src`), donc aucun changement frontend requis. Note : `CmtBatchController` (`/test/batch/cmt`) et `FunctionalLinesBatchController` (`/test/batch/functional-lines`) utilisent le même préfixe `/test/` — non renommés ici (hors périmètre du sweep Leasing), à traiter dans un futur sweep CMT / LigneFonctionnelle pour cohérence globale.

---

### Endpoints — `LeasingConfigController` (base `/leasing-config`) — nouveau

| Méthode | URL | `@PreAuthorize` (classe) | Justification |
|---|---|---|---|
| GET | `/leasing-config` | `hasAuthority('SYSTEM_ADMIN')` | Lecture de la configuration (valeur résiduelle, taux TVA, seuils d'alerte échéances à venir). |
| PUT | `/leasing-config` | `hasAuthority('SYSTEM_ADMIN')` | Modification — mêmes règles d'accès que `/commission-config` (paramètres métier globaux). |

Créé pendant le sweep Leasing (2026-07-04) pour remplacer `leasing.contract.valeur-residuelle` / `leasing.contract.taux-tva`, auparavant codés en dur dans `application.yml` (violation `conventions/configuration.md`).

> **Étendu le 2026-07-04 (sweep CMT)** : `LeasingConfig` complété avec `alerteEcheanceJoursCourtTerme`/`alerteEcheanceJoursLongTerme` (mêmes seuils créés côté CMT via `CmtConfig`, cf. `docs/rbac/cmt.md`) — remplace les valeurs `plusDays(7)`/`plusDays(30)` auparavant codées en dur dans `LeasingMetrics`, découvert lors de l'audit de conformité 100% du module CMT.

---

### Guards Angular (`leasing-routing.module.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/leasing/organismesLeasing` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/leasing/contratsCadres` | `departmentGuard('Finance')` | Idem. |
| `/portail/leasing/contratsLeasing` (parent — `encours`, `en-attente-de-ML`, `archives`, `creation/*`, `:id/tableau-amortissement`) | `departmentGuard('Finance')` | Idem. |
| `/portail/leasing/echeances/edit/:id` — nouveau (21/09/2026) | `departmentGuard('Finance')` + `systemAdminGuard` (les deux posés directement sur la route) | `SYSTEM_ADMIN` uniquement — remplace le dialogue d'édition d'échéance de `TableauAmortissementComponent`, même restriction que le `PUT /echeances-leasing/{id}` (`ADMIN_ONLY`) qu'elle appelle. Atteignable aussi depuis `RecapitulatifComponent` (`CREATE_EDIT_ELIGIBILITY[LEASING]`), qui vérifie `authService.isSystemAdmin()` avant d'afficher le bouton « Éditer ». |
| `/portail/admin/leasing-config` (parent `admin.routes.ts`) | `systemAdminGuard` (posé au niveau de `/portail/admin` dans `portail.routes.ts`) | `SYSTEM_ADMIN` uniquement. |

**Avance d'un contrat (nouveau 19/10/2026, refondu 20/10/2026)** — pas de nouvelle route : le choix du moyen de paiement se fait dans l'étape « Détails généraux » du wizard de création / modification d'un contrat (`creation/*`, `departmentGuard('Finance')`), et la carte « Avance » du `TableauAmortissementComponent` est en lecture seule. Le moyen de paiement est transmis avec le contrat (`POST/PUT /contratsLeasing`, `FINANCE_OR_ADMIN`) ; il n'existe plus de point d'entrée de rattachement séparé. Conditions d'affichage :

- colonne Actions du tableau (bascule de statut et crayon d'édition, `isAdmin()`) : remplacée par « Suit son moyen de paiement » pour une échéance rattachée (`moyenPaiementId` non nul), car le backend refuse ces deux actions (`toggle-status`, `PUT /{id}`) sur une avance rattachée — divergence volontaire d'affichage, le backend reste la garde.

Aucun endpoint n'est ajouté aux modules Chèques / LCN / Virements : leur lecture et la mention d'avance sur leurs textes passent par des façades internes (`ChequeAvanceReader`, `LcnAvanceReader`, `VirementAvanceReader`) et par `ITransactionService.modifierTextes` pour la transaction, sans exposition HTTP.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Leasing" (section) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |
| "Les Contrats encours" | idem |
| "Les Contrats en attente de main levées" | idem |
| "Les Contrats archivés" | idem |
| "Les organismes leasing" | idem |
| "Les Contrats cadres" | idem |
| "Configuration leasing" (sous "Administration") | `SYSTEM_ADMIN` (hérité de la section "Administration") |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (organisme leasing) | `isSystemAdmin()` | `OrganismeLeasingListComponent` |
| Bouton "Supprimer" (contrat de leasing) | `isSystemAdmin()` | `ContratsLeasingListComponent` (vue "encours") |
| Créer / Modifier / Archiver un contrat cadre | `canModify()` = `isSystemAdmin() \|\| belongsToDepartment('Finance')` | `ContratCadreListComponent` |
| Toggle statut échéance (PREVU ↔ PAYE) + édition échéance | `isAdmin()` | `TableauAmortissementComponent` |

**Pas de divergence assumée backend/frontend** sur ce module : les guards de route et les conditions d'affichage reflètent exactement les `@PreAuthorize` backend.

**Corrigé (2026-07-04)** : `OrganismeLeasingListComponent.viewContratsCadres(organismeId)` ignorait son paramètre et naviguait vers la liste non filtrée des contrats cadres. Corrigé en propageant `organismeId` en query param (`/portail/leasing/contratsCadres?organismeId=...`), lu par `ContratCadreListComponent` via `ActivatedRoute` pour pré-remplir `searchForm` au chargement — pattern déjà utilisé ailleurs dans le repo (ex. `banqueId` en query param sur les formulaires de transaction).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter organismes / contrats cadres / contrats leasing / échéances | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Créer / modifier un organisme, un contrat cadre, un contrat leasing | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Archiver un contrat cadre | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Clôturer un contrat leasing (main levée) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Supprimer un organisme ou un contrat de leasing | `SYSTEM_ADMIN` uniquement |
| Modifier / basculer le statut d'une échéance | `SYSTEM_ADMIN` uniquement |
| Déclencher manuellement le batch Main Levée | `SYSTEM_ADMIN` uniquement |
| Consulter / modifier la configuration leasing (valeur résiduelle, TVA) | `SYSTEM_ADMIN` uniquement |

---

### Point hors périmètre — corrigé a posteriori (2026-07-04)

`ValidationController` (`/rapprochement/*`, module `validation`, hors périmètre `leasing/`) exposait alors `/rapprochement/process/echeance-leasing`, qui délègue à `LeasingValidationService.processLeasingTransaction()`. Signalé initialement comme **sans aucune annotation `@PreAuthorize`** — ni au niveau classe ni au niveau méthode — comme les ~24 autres endpoints `/rapprochement/process/*` de ce même controller (CMT, virements, chèques, LCN, agios, commission, etc.) qui existaient à l'époque.

**Corrigé (décision utilisateur explicite)** : `@PreAuthorize` posé au niveau classe sur `ValidationController` avec la même expression `FINANCE_OR_ADMIN` (`hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')`) que les autres controllers du repo, plus `@SecurityRequirement(name = "bearerAuth")`. Ce correctif sécurise les 25 endpoints du controller d'un coup (pas seulement celui du leasing), puisqu'ils partageaient tous la même faille.

Cette correction a été appliquée dans le cadre du sweep Leasing car elle touchait directement son endpoint de validation. `ValidationController` a depuis été entièrement consolidé (19/09/2026, voir Chapitre 28 — Validation) : il n'expose plus que `POST /rapprochement/process` et `DELETE /rapprochement/annuler/{id}`, tous deux sous le même `@PreAuthorize` de classe — le détail par endpoint promis ci-dessus n'a donc plus lieu d'être, il n'y a plus qu'un seul endpoint à documenter.

*Ces financements interviennent souvent dans le cadre d'un Marché ; les fiches suivantes couvrent les Marchés et leurs modules satellites.*

<a id="chap5"></a>
## Chapitre 5 — Marchés

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN  = "hasAuthority('SYSTEM_ADMIN')";
private static final String AUTHENTICATED = "isAuthenticated()";
```

---

### Endpoints — `MarcheController` (base `/marches`) — corrigé (2026-07-10, RBAC inchangé au sweep du 12/07/2026)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/marches` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| GET | `/marches/{id}` | `AUTHENTICATED` | Détail — consulté aussi par d'autres modules (décomptes, préfinancements, etc.). |
| GET | `/marches` | `AUTHENTICATED` | Liste paginée avec filtre `actif` (Actif / Archivé / Tous). |
| PUT | `/marches/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| PATCH | `/marches/{id}/archiver` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-10) — archive le marché (exclusion définitive des calculs agios/encours). |
| PATCH | `/marches/{id}/desarchiver` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-10) — réactive le marché. |
| DELETE | `/marches/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Bloquée si le marché a des décomptes, préfinancements, droits constatés, factorings ou lignes spécifiques liés (sinon suppression en cascade, `cascade=ALL, orphanRemoval=true` sur `Marche`) — utiliser l'archivage dans ce cas. |
| GET | `/marches/export/pdf` | `AUTHENTICATED` | Nouveau (2026-07-22) — export PDF de la liste filtrée (mêmes filtres que `GET /marches`, ignore la pagination), même niveau d'autorisation que la lecture. |

> **Corrigé le 2026-07-10** : le controller n'avait initialement **aucun** `@PreAuthorize` — tous
> les endpoints (y compris suppression) étaient accessibles à quiconque possédait un token valide.
> Ajout complet du RBAC, mirroring `BanqueController`/`LigneFonctionnelleController`. À cette
> occasion, ajout du champ `actif` (archivage) et d'un guard sur la suppression physique (absent
> initialement — un marché avec des données financières liées pouvait être supprimé en cascade
> sans avertissement).

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/marches` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/lignes-fonctionnelles-specifiques/marche/:id` (écran "lignes par marché", `FunctionalLinesByMarcheComponent`) | `departmentGuard('Finance')` | Idem. |

> **Corrigé le 2026-07-10** : la route `/portail/marches` n'avait initialement **aucun guard**. Ajout de
> `departmentGuard('Finance')`, cohérent avec `agios`/`cmt-routing.ts`/`leasing-routing.module.ts`.
>
> **Corrigé le 12/07/2026 (sweep de conformité)** : la route
> `/portail/lignes-fonctionnelles-specifiques/marche/:id` — qui affiche les lignes fonctionnelles
> (générales et spécifiques) rattachées à un marché, donnée financière Finance — n'avait **aucun**
> `canActivate`, contrairement à la route parente `/portail/marches`. Trouvé lors de ce sweep (hors
> du périmètre initialement vérifié par l'audit d'archivage du 10/07), corrigé par ajout de
> `departmentGuard('Finance')`, cohérent avec `lcn-encours/:lineId` (même famille de route "détail
> par entité").
>
> **Corrigé le 12/07/2026 (même journée, correctif dédié post-sweep)** : les modules sœurs de la
> section "Les Marchés" — `préfinancements`, `droits-constate`, `factoring`, `decomptes` — avaient
> le même problème (aucun guard sur leurs routes, aucun `@PreAuthorize` sur leurs controllers,
> aucun `requiredAuthority` sidebar). RBAC corrigé pour les 4 modules — voir `prefinancements.md`,
> `factoring.md`, `decomptes.md`, `droits-constate.md`. Seul le RBAC a été traité pour ces 4
> modules (pas de conformité complète — encapsulation, DTOs, audit Envers, doc fonctionnelle
> restent ouverts).

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "La liste des marchés" (sous "Les Marchés") | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

> **Corrigé le 2026-07-10** : l'item n'avait initialement aucun `requiredAuthority`. Les autres
> items de la section ("Les préfinancements", "Les droits constatés", "Factoring", "Les décomptes")
> avaient le même gap — corrigé le 12/07/2026 (voir `prefinancements.md`, `factoring.md`,
> `decomptes.md`, `droits-constate.md`).

### Actions conditionnées dans l'UI (`marches-list.component.ts`)

| Action | Condition d'affichage |
|---|---|
| Bouton "Supprimer" (corbeille) | `isAdmin()` (`isSystemAdmin()`) — cohérent avec le backend (`SYSTEM_ADMIN` sur `DELETE`). |
| Boutons "Archiver" / "Désarchiver" | Visibles pour tout utilisateur ayant accès à la page (Finance ou `SYSTEM_ADMIN`) — cohérent avec le backend (`FINANCE_OR_ADMIN` sur les deux endpoints PATCH). |

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter un marché ou la liste | Tout utilisateur authentifié (backend) ; en pratique restreint à Finance/Admin par le guard de route | ✅ conforme |
| Créer / modifier un marché | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme |
| Archiver / désarchiver un marché | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme |
| Supprimer définitivement un marché | `SYSTEM_ADMIN` uniquement, bloqué si des données financières sont liées | ✅ conforme |

<a id="chap6"></a>
## Chapitre 6 — Caution

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `CautionController` (base `/cautions`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/cautions` | `FINANCE_OR_ADMIN` | Création réservée à l'équipe Finance ou l'admin système. |
| GET | `/cautions/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/cautions` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée avec filtres. |
| GET | `/cautions/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — mêmes filtres que la recherche, sans pagination. Paramètre optionnel `colonnes` (nouveau, 23/07/2026) : restreint les colonnes incluses dans le document ; absent, vide, ou sans valeur reconnue ⇒ toutes les colonnes. N'affecte pas le niveau d'autorisation. |
| GET | `/cautions/export/excel` | `FINANCE_OR_ADMIN` | Export Excel (.xlsx) (nouveau, 20/09/2026) — mêmes filtres, mêmes règles pour `colonnes` et même niveau d'autorisation que `/cautions/export/pdf` ; seul le format de sortie diffère. Généré via `StaportExcelReportService` (Apache POI), pendant transversal de `StaportPdfReportService` dans `staport.finance.gBanque.reports.service`. |
| PUT | `/cautions/{id}` | `FINANCE_OR_ADMIN` | Modification — autorisée même si la caution est clôturée (nouveau, 20/09/2026) : recalcule alors automatiquement la commission de clôture si un champ qui l'influence change, bloquée en service (exception métier, pas RBAC) si cette commission est déjà `PAYE` ou introuvable. N'affecte pas le niveau d'autorisation. |
| DELETE | `/cautions/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Bloquée en service si la caution est clôturée. |
| POST | `/cautions/{id}/cloturer` | `FINANCE_OR_ADMIN` | Clôture (main levée) — génère la commission de clôture (+ TVA si requise par la banque). |
| POST | `/cautions/generer-commissions` | `FINANCE_OR_ADMIN` | Déclenchement manuel de la génération mensuelle des commissions. |

> **Écran "TVA sur cautions"** : n'a pas de contrôleur dédié — il consomme `ImpotEtTaxeController`
> (filtre `motif=TVA_COMMISSION_CAUTION`), y compris son bouton "Exporter PDF" (nouveau,
> 2026-07-22, `GET /impots-taxes/export/pdf`). RBAC détaillé au chapitre 20 — Impôts et Taxes.

---

### Endpoints — `CautionCommissionConfigController` (base `/cautions/commission-config`) — nouveau

| Méthode | URL | `@PreAuthorize` (classe) | Justification |
|---|---|---|---|
| GET | `/cautions/commission-config` | `SYSTEM_ADMIN` | Lecture des paramètres de commission (seuil minimum, diviseur) par banque. |
| POST | `/cautions/commission-config` | `SYSTEM_ADMIN` | Création — une seule configuration par banque (contrainte d'unicité). |
| PUT | `/cautions/commission-config/{id}` | `SYSTEM_ADMIN` | Modification. |
| DELETE | `/cautions/commission-config/{id}` | `SYSTEM_ADMIN` | Suppression. |

Créé pendant le sweep de conformité 100% Caution (2026-07-12) pour remplacer les constantes Java
auparavant codées en dur dans chaque `CommissionCautionStrategie` (seuil minimum, diviseur du
prorata journalier) — voir `conventions/configuration.md`. Cohérent avec `/commission-config`
(module `commision`), réservé au même niveau d'accès.

> **Point d'attention opérationnel** : cette table (`caution_commission_config`) est vide tant
> qu'un administrateur n'a pas créé une configuration pour chaque banque (AWB, BMCI, CDM, SAHAM
> BANK — valeurs de référence dans `docs/modules/caution.md`, section 7). Sans configuration pour
> une banque, toute génération de commission pour ses cautions échoue explicitement
> (`CautionBusinessException`, HTTP 422) plutôt que d'utiliser une valeur par défaut silencieuse.

#### Écran Angular associé — `/portail/admin/caution-commission-config`

Créé après ce sweep (`CautionCommissionConfigListComponent`, mirroring
`CommissionConfigListComponent` du module `commision`). Route déclarée dans `admin.routes.ts`,
sous `/admin` — protégée par `systemAdminGuard` au niveau parent (`portail.routes.ts`), pas besoin
de guard dédié sur la route enfant. Entrée sidebar "Commissions sur cautions" ajoutée sous la
section "Administration" (déjà `requiredAuthority: 'SYSTEM_ADMIN'` au niveau de la section).

CRUD complet (liste, création, modification, suppression) sur `/cautions/commission-config`. À la
création, seules les banques n'ayant pas encore de configuration sont proposées dans le menu
déroulant (contrainte d'unicité backend — une configuration par banque).

---

### Guards Angular (`caution-routing.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/cautions` (liste) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/cautions/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/cautions/edit/:id` | `departmentGuard('Finance')` | Idem. |
| `/portail/cautions/commissions` | `departmentGuard('Finance')` | Idem. |
| `/portail/cautions/tva` | `departmentGuard('Finance')` | Idem. |

**Corrigé pendant ce sweep** : les routes utilisaient auparavant `systemAdminGuard` (importé mais
non appliqué) au lieu de `departmentGuard('Finance')` — incohérent avec le reste du module
(sidebar, actions UI) qui autorisait déjà toute l'équipe Finance. Remplacé par
`departmentGuard('Finance')` sur les 5 routes, alignant le guard sur le comportement réel attendu.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Cautions" (section) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |
| "Liste des cautions" | idem |
| "Commissions sur cautions" | idem |
| "TVA sur cautions" | idem |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (caution) | `isSystemAdmin()` | `CautionListComponent` |

**Corrigé pendant ce sweep** : `CautionListComponent` utilisait `authService.isAdmin()` (méthode
dépréciée) au lieu de `authService.isSystemAdmin()`. Corrigé — propriété renommée `isAdmin` →
`isSystemAdmin` (usage cohérent avec le reste du repo). Même correction appliquée à
`CommissionCautionListComponent` et `TvaCautionListComponent` (propriété conservée `isAdmin` dans
ces deux composants — usage extensif dans les templates — seul l'appel sous-jacent a été corrigé
vers `isSystemAdmin()`).

**Pas de divergence assumée backend/frontend** sur ce module : les guards de route et les
conditions d'affichage reflètent exactement les `@PreAuthorize` backend.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter, créer, modifier, clôturer une caution | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Générer manuellement les commissions du mois | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Supprimer définitivement une caution | `SYSTEM_ADMIN` uniquement |
| Consulter / modifier la configuration des commissions (seuils, diviseur) | `SYSTEM_ADMIN` uniquement |

<a id="chap7"></a>
## Chapitre 7 — Décomptes

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `DecompteController` (base `/decomptes`) — corrigé le 12/07/2026

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/decomptes` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| PUT | `/decomptes/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| DELETE | `/decomptes/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Refusée en plus si le décompte n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/decomptes/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/decomptes` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée avec filtres. |
| PATCH | `/decomptes/{id}/toggle` | `SYSTEM_ADMIN` | Changement de statut PREVU/PAYE — cohérent avec la correction déjà appliquée sur ce type d'action dans les modules Moyens de paiement et CMT (échéances). |
| GET | `/decomptes/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF — mêmes filtres que la recherche, sans pagination. |
| GET | `/decomptes/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de décomptes. |
| GET | `/decomptes/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de décomptes filtré, sans pagination. Seul export manquant du module (liste déjà couverte) — Décompte n'a pas de séparation remboursement/déblocage contrairement à Préfinancement/Factoring/Droit Constaté (l'encaissement est son seul flux). |

> **Corrigé le 12/07/2026 (sweep de conformité Marchés, section "modules sœurs")** : le controller
> n'avait initialement **aucun** `@PreAuthorize` — tous les endpoints (y compris suppression,
> export PDF et changement de statut) étaient accessibles à quiconque possédait un token valide.
> Ajout complet du RBAC, mirroring `CautionController`.

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/decomptes` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |

> **Corrigé le 12/07/2026** : la route n'avait aucun guard. Ajout de `departmentGuard('Finance')`,
> cohérent avec `marches`/`agios`.

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les décomptes" (sous "Les Marchés") | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

> **Corrigé le 12/07/2026** : l'item n'avait initialement aucun `requiredAuthority`.

### Actions conditionnées dans l'UI (`decomptes-list.component.ts`)

| Action | Condition d'affichage |
|---|---|
| Bouton "Supprimer" | `@if (isAdmin)` (propriété alimentée par `authService.isAdmin()`). |
| Interrupteur de statut (PREVU/PAYE) | `@if (isAdmin)`. |

---

### Conformité (12/07/2026)

Sweep de conformité complet effectué : encapsulation des domaines (`IMarcheService`/
`IBanqueService` au lieu des Repository directs), DTOs records (`dtos/request`+`dtos/response`),
mapper MapStruct, `Decompte` `@Audited` + `@AuditableAction` sur create/update/delete, Swagger,
métriques, correction N+1 (`@EntityGraph`) et calcul d'encours par requête `SUM` (plus de somme en
mémoire). Détail dans `docs/modules/decomptes.md` et la mémoire `modules_sweep`.

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter, créer, modifier un décompte | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Exporter en PDF | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Changer le statut PREVU/PAYE | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |
| Supprimer définitivement un décompte | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |

<a id="chap8"></a>
## Chapitre 8 — Préfinancements

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `PrefinancementController` (base `/prefinancements`) — corrigé le 12/07/2026

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/prefinancements` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| PUT | `/prefinancements/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| DELETE | `/prefinancements/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Refusée en plus si le préfinancement n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/prefinancements/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/prefinancements` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée avec filtres. |
| PATCH | `/prefinancements/{id}/status/toggle` | `SYSTEM_ADMIN` | Changement de statut PREVU/PAYE — cohérent avec la correction déjà appliquée sur ce type d'action dans les modules Moyens de paiement et CMT (échéances). |
| GET | `/prefinancements/remboursement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de remboursement. |
| GET | `/prefinancements/deblocage/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de déblocage. |
| GET | `/prefinancements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de la liste filtrée (mêmes filtres que `GET /prefinancements`, ignore la pagination). |
| GET | `/prefinancements/remboursement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de remboursement filtré. |
| GET | `/prefinancements/deblocage/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de déblocage filtré. |

> **Corrigé le 12/07/2026 (sweep de conformité Marchés, section "modules sœurs")** : le controller
> n'avait initialement **aucun** `@PreAuthorize` — tous les endpoints (y compris suppression et
> changement de statut) étaient accessibles à quiconque possédait un token valide. Ajout complet du
> RBAC, mirroring `CautionController`.

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/préfinancements` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/prefi-encours/:lineId` (écran "encours" consulté depuis une ligne fonctionnelle) | `departmentGuard('Finance')` | Idem. |

> **Corrigé le 12/07/2026** : aucune des deux routes n'avait de guard. Ajout de
> `departmentGuard('Finance')`, cohérent avec `marches`/`agios`/`lcn-encours`.

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les préfinancements" (sous "Les Marchés") | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

> **Corrigé le 12/07/2026** : l'item n'avait initialement aucun `requiredAuthority`.

### Actions conditionnées dans l'UI (`prefinancements-list.component.ts`)

| Action | Condition d'affichage |
|---|---|
| Bouton "Supprimer" | `authService.isAdmin()` — déjà correct avant ce sweep. |
| Interrupteur de statut (PREVU/PAYE) | `@if (authService.isAdmin())` (corrigé le 12/07/2026, cohérent avec le backend). |

---

### Conformité (12/07/2026)

Sweep de conformité complet effectué : encapsulation des domaines (`IMarcheService`/
`INatureLigneFonctionnelleService` au lieu des Repository directs), DTOs records (`dtos/request`+
`dtos/response`), mapper MapStruct, `Prefinancement` `@Audited` (relations `ligneFonctionnelle`/
`ligneFonctionnelleSpecifique` en `NOT_AUDITED`) + `@AuditableAction` sur create/update/delete,
Swagger, métriques, correction N+1 (`@EntityGraph`) et calcul des encours (déblocage/remboursement)
par requête `SUM` (plus de somme en mémoire). `AgregatedSuiviePrefiLigneResponseDto`/
`SuiviePrefiLigneResponseDto` convertis en records (consommés par `LigneFonctionnelleService`/
`LigneFonctionnelleSpecifiqueService` pour l'écran "suivi de ligne"). Détail dans
`docs/modules/prefinancements.md` et la mémoire `modules_sweep`.

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter, créer, modifier un préfinancement | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Consulter les encours (déblocage/remboursement) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Changer le statut PREVU/PAYE | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |
| Supprimer définitivement un préfinancement | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |

<a id="chap9"></a>
## Chapitre 9 — Factoring

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `FactoringController` (base `/factoring`) — corrigé le 12/07/2026

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/factoring` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| PUT | `/factoring/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| DELETE | `/factoring/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Refusée en plus si le factoring n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/factoring/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/factoring` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée avec filtres. |
| PATCH | `/factoring/{id}/toggleStatus` | `SYSTEM_ADMIN` | Changement de statut PREVU/PAYE — cohérent avec la correction déjà appliquée sur ce type d'action dans les modules Moyens de paiement et CMT (échéances). |
| GET | `/factoring/remboursement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de remboursement. |
| GET | `/factoring/deblocage/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de déblocage. |
| GET | `/factoring/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de la liste filtrée (mêmes filtres que `GET /factoring`, ignore la pagination). |
| GET | `/factoring/remboursement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de remboursement filtré. |
| GET | `/factoring/deblocage/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de déblocage filtré. |

> **Corrigé le 12/07/2026 (sweep de conformité Marchés, section "modules sœurs")** : le controller
> n'avait initialement **aucun** `@PreAuthorize` — tous les endpoints (y compris suppression et
> changement de statut) étaient accessibles à quiconque possédait un token valide. Ajout complet du
> RBAC, mirroring `CautionController`.

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/factoring` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/factoring-encours/:lineId` (écran "encours" consulté depuis une ligne fonctionnelle) | `departmentGuard('Finance')` | Idem. |

> **Corrigé le 12/07/2026** : aucune des deux routes n'avait de guard. Ajout de
> `departmentGuard('Finance')`, cohérent avec `marches`/`agios`/`lcn-encours`.

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Factoring" (sous "Les Marchés") | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

> **Corrigé le 12/07/2026** : l'item n'avait initialement aucun `requiredAuthority`.

### Actions conditionnées dans l'UI (`factoring-list.component.ts`)

| Action | Condition d'affichage |
|---|---|
| Bouton "Supprimer" | `@if (authService.isAdmin())` (corrigé le 12/07/2026, cohérent avec le backend). |
| Interrupteur de statut (PREVU/PAYE) | `@if (authService.isAdmin())` (corrigé le 12/07/2026, cohérent avec le backend). |

---

### Conformité (12/07/2026)

Sweep de conformité complet effectué : encapsulation des domaines (`IMarcheService`/
`INatureLigneFonctionnelleService` au lieu des Repository directs), DTOs records (`dtos/request`+
`dtos/response`), mapper MapStruct, `Factoring` `@Audited` (relations `ligneFonctionnelle`/
`ligneFonctionnelleSpecifique` en `NOT_AUDITED`) + `@AuditableAction` sur create/update/delete,
Swagger, métriques, correction N+1 (`@EntityGraph`) et calcul des encours (déblocage/remboursement)
par requête `SUM` (plus de somme en mémoire). `AgregatedSuivieFactoringLigneResponseDto`/
`SuivieFactoringLineResponseDto` convertis en records (consommés par `LigneFonctionnelleService`/
`LigneFonctionnelleSpecifiqueService` pour l'écran "suivi de ligne"). Détail dans
`docs/modules/factoring.md` et la mémoire `modules_sweep`.

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter, créer, modifier un factoring | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Consulter les encours (déblocage/remboursement) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Changer le statut PREVU/PAYE | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |
| Supprimer définitivement un factoring | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |

<a id="chap10"></a>
## Chapitre 10 — Droits Constatés

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `DroitConstateController` (base `/droitsConstate`) — corrigé le 12/07/2026

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/droitsConstate` | `FINANCE_OR_ADMIN` | Création — donnée financière propre au département Finance. |
| PUT | `/droitsConstate/{id}` | `FINANCE_OR_ADMIN` | Modification. |
| DELETE | `/droitsConstate/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Refusée en plus si le droit constaté n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/droitsConstate/{id}` | `FINANCE_OR_ADMIN` | Lecture. |
| GET | `/droitsConstate` (recherche) | `FINANCE_OR_ADMIN` | Liste paginée avec filtres. |
| PATCH | `/droitsConstate/{id}/status/toggle` | `SYSTEM_ADMIN` | Changement de statut PREVU/PAYE — cohérent avec la correction déjà appliquée sur ce type d'action dans les modules Moyens de paiement et CMT (échéances). |
| GET | `/droitsConstate/remboursement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de remboursement. |
| GET | `/droitsConstate/deblocage/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Consultation des encours de déblocage. |
| GET | `/droitsConstate/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de la liste filtrée (mêmes filtres que `GET /droitsConstate`, ignore la pagination). |
| GET | `/droitsConstate/remboursement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de remboursement filtré. |
| GET | `/droitsConstate/deblocage/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (2026-07-22) — export PDF de l'encours de déblocage filtré. |

> **Corrigé le 12/07/2026 (sweep de conformité Marchés, section "modules sœurs")** : le controller
> n'avait initialement **aucun** `@PreAuthorize` — tous les endpoints (y compris suppression et
> changement de statut) étaient accessibles à quiconque possédait un token valide. Ajout complet du
> RBAC, mirroring `CautionController`.

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/droits-constate` (parent — liste, `new`, `edit/:id`) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/dc-encours/:lineId` (écran "encours" consulté depuis une ligne fonctionnelle) | `departmentGuard('Finance')` | Idem. |

> **Corrigé le 12/07/2026** : aucune des deux routes n'avait de guard. Ajout de
> `departmentGuard('Finance')`, cohérent avec `marches`/`agios`/`lcn-encours`.

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les droits constatés" (sous "Les Marchés") | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

> **Corrigé le 12/07/2026** : l'item n'avait initialement aucun `requiredAuthority`.

### Actions conditionnées dans l'UI (`droit-constate-list.component.ts`)

| Action | Condition d'affichage |
|---|---|
| Bouton "Supprimer" | `@if (authService.isAdmin())` (corrigé le 12/07/2026, cohérent avec le backend). |
| Interrupteur de statut (PREVU/PAYE) | `@if (authService.isAdmin())` (corrigé le 12/07/2026, cohérent avec le backend). |

---

### Conformité (13/07/2026)

Sweep de conformité complet effectué : encapsulation des domaines (`IMarcheService`/
`IBanqueService`/`INatureLigneFonctionnelleService` au lieu des Repository directs), DTOs records
(`dtos/request`+`dtos/response`), mapper MapStruct, `DroitConstate` `@Audited` (relations
`ligneFonctionnelle`/`ligneFonctionnelleSpecifique` en `NOT_AUDITED`) + `@AuditableAction` sur
create/update/delete, Swagger, métriques, correction N+1 (`@EntityGraph` sur `marche`+`banque`,
plat — pas de sous-graphe requis, `DroitConstate.banque` étant un champ direct de l'entité) et
calcul des encours (déblocage/remboursement) par requête `SUM` (plus de somme en mémoire).
`AgregatedSuivieDcLigneResponseDto`/`SuivieDcLigneResponseDto` convertis en records (consommés par
`LigneFonctionnelleService`/`LigneFonctionnelleSpecifiqueService` pour l'écran "suivi de ligne").
Ancien helper `resolveBanque` (null-check manuel redondant avec `@NotNull` sur `banqueId`) supprimé.
Détail dans `docs/modules/droits-constate.md` et la mémoire `modules_sweep`.

**Anomalie hors périmètre repérée, non corrigée** : `droitConstate/dtos/AgregatedSuivieLcnLigneResponseDto.java`
est un DTO actif du module LCN, mal rangé dans le package `droitConstate.dtos` — laissé intact
pendant ce sweep (aucune modification demandée sur ce fichier), voir `TODO.md`.

---

### Résumé

| Action | Qui peut faire | État |
|---|---|---|
| Consulter, créer, modifier un droit constaté | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Consulter les encours (déblocage/remboursement) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` | ✅ conforme (12/07/2026) |
| Changer le statut PREVU/PAYE | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |
| Supprimer définitivement un droit constaté | `SYSTEM_ADMIN` uniquement | ✅ conforme (12/07/2026) |

*Les opérations liées aux comptes et aux marchés s'exécutent via les moyens de paiement suivants.*

<a id="chap11"></a>
## Chapitre 11 — Chèque

---

### Endpoints (`ChequeController`, base `/cheques`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celles
explicitement réannotées.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/cheques` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création (encaissement) réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/cheques/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| GET | `/cheques/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/cheques/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/cheques/decaissements` | `FINANCE_OR_ADMIN` | Liste réservée à l'équipe Finance ou à l'admin système. |
| GET | `/cheques/encaissements` | `FINANCE_OR_ADMIN` | Idem. |
| PATCH | `/cheques/toggle/{id}` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/cheques/decaissements/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/cheques/encaissements/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/cheques/encaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des encaissements filtrée, sans pagination. |
| GET | `/cheques/decaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des décaissements filtrée, sans pagination. |
| GET | `/cheques/encaissements/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours d'encaissement filtré, sans pagination. |
| GET | `/cheques/decaissements/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours de décaissement filtré, sans pagination. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`).

---

### Guards Angular (`cheques.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/Cheques/decaissements` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/Cheques/encaissements` | `departmentGuard('Finance')` | Idem. |
| `/portail/Cheques/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/Cheques/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les moyens de paiement" (section parente) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Chèques" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Chèques" → "Encaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Chèques" → "Décaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (listes encaissement/décaissement) | `isAdmin` (`authService.isSystemAdmin()`) | `ChequesDecaissementsListComponent`, `ChequesEncaissementsListComponent` |
| Commutateur de statut (ENCOURS ↔ REGLE) | `isAdmin` (`authService.isSystemAdmin()`) | `ChequesDecaissementsListComponent`, `ChequesEncaissementsListComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un chèque (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un chèque | `SYSTEM_ADMIN` uniquement |

<a id="chap12"></a>
## Chapitre 12 — LCN

---

### Endpoints (`LcnController`, base `/lcns`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celles
explicitement réannotées.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/lcns` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création (encaissement) réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/lcns/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| GET | `/lcns/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/lcns/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/lcns/decaissements` | `FINANCE_OR_ADMIN` | Liste réservée à l'équipe Finance ou à l'admin système. |
| GET | `/lcns/encaissements` | `FINANCE_OR_ADMIN` | Idem. |
| PATCH | `/lcns/toggle/{id}` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/lcns/decaissements/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/lcns/encaissements/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/lcns/encaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des encaissements filtrée, sans pagination. |
| GET | `/lcns/decaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des décaissements filtrée, sans pagination. |
| GET | `/lcns/encaissements/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours d'encaissement filtré, sans pagination. |
| GET | `/lcns/decaissements/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours de décaissement filtré, sans pagination. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`).

---

### Guards Angular (`lcn.routes.ts` + `portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/LCN/decaissements` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/LCN/encaissements` | `departmentGuard('Finance')` | Idem. |
| `/portail/LCN/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/LCN/edit/:id` | `departmentGuard('Finance')` | Idem. |
| `/portail/lcn-encours/:lineId` (suivi escompte par ligne fonctionnelle) | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "LCN" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "LCN" → "Encaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "LCN" → "Décaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (liste décaissements) | `isAdmin` (`authService.isSystemAdmin()`) | `LcnDecaissementsListComponent` |
| Bouton "Supprimer" (liste encaissements) | `isAdmin` (`authService.isSystemAdmin()`) | `LcnEncaissementsListComponent` |
| Commutateur de statut (ENCOURS ↔ REGLE) | `isAdmin` (`authService.isSystemAdmin()`) | `LcnDecaissementsListComponent`, `LcnEncaissementsListComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un LCN (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un LCN | `SYSTEM_ADMIN` uniquement |

<a id="chap13"></a>
## Chapitre 13 — Virement

---

### Endpoints (`VirementController`, base `/virements`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celles
explicitement réannotées.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/virements` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création (encaissement) réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/virements/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| GET | `/virements/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/virements/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/virements/encaissements` | `FINANCE_OR_ADMIN` | Liste réservée à l'équipe Finance ou à l'admin système. |
| GET | `/virements/decaissements` | `FINANCE_OR_ADMIN` | Idem. |
| PATCH | `/virements/toggle/{id}` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/virements/decaissement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/virements/encaissement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/virements/encaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des encaissements filtrée, sans pagination. |
| GET | `/virements/decaissements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste des décaissements filtrée, sans pagination. |
| GET | `/virements/encaissement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours d'encaissement filtré, sans pagination. |
| GET | `/virements/decaissement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours de décaissement filtré, sans pagination. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`).

---

### Guards Angular (`virements.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/Virements/decaissement` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/Virements/encaissement` | `departmentGuard('Finance')` | Idem. |
| `/portail/Virements/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/Virements/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Virements nationaux" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Virements nationaux" → "Encaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Virements nationaux" → "Décaissements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

L'entrée "Virements internationaux" (module distinct) n'est pas couverte par cette fiche — voir
`docs/rbac/virement-international.md`.

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (liste décaissements) | `isAdmin` (`authService.isSystemAdmin()`) | `VirementDecaissementListComponent` |
| Bouton "Supprimer" (liste encaissements) | `isAdmin` (`authService.isSystemAdmin()`) | `VirementEncaissementListComponent` |
| Commutateur de statut (ENCOURS ↔ REGLE) | `isAdmin` (`authService.isSystemAdmin()`) | `VirementDecaissementListComponent`, `VirementEncaissementListComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un virement (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un virement | `SYSTEM_ADMIN` uniquement |

<a id="chap14"></a>
## Chapitre 14 — Virement International

---

### Endpoints (`VirementInternationalController`, base `/virements-internationaux`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celles
explicitement réannotées.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/virements-internationaux` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/virements-internationaux/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| GET | `/virements-internationaux/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/virements-internationaux/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/virements-internationaux` | `FINANCE_OR_ADMIN` | Liste/recherche réservée à l'équipe Finance ou à l'admin système. |
| GET | `/virements-internationaux/encaissements` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/virements-internationaux/decaissements` | `FINANCE_OR_ADMIN` | Idem. |
| PATCH | `/virements-internationaux/toggle/{id}` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/virements-internationaux/decaissement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/virements-internationaux/encaissement/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/virements-internationaux/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste combinée filtrée (mêmes filtres que `GET /virements-internationaux`), sans pagination. Seul écran de liste consommé par le frontend — les endpoints séparés `/encaissements` et `/decaissements` n'ont pas d'écran dédié et n'ont donc pas reçu d'export. |
| GET | `/virements-internationaux/encaissement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours d'encaissement filtré, sans pagination. |
| GET | `/virements-internationaux/decaissement/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours de décaissement filtré, sans pagination. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`).

---

### Guards Angular (`virement-international.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/virements-internationaux` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/virements-internationaux/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/virements-internationaux/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Virements internationaux" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (liste) | `isAdmin` (`authService.isSystemAdmin()`) | `VirementInternationalListComponent` |
| Commutateur de statut (ENCOURS ↔ REGLE) | `isAdmin` (`authService.isSystemAdmin()`) | `VirementInternationalListComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un virement international (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un virement international | `SYSTEM_ADMIN` uniquement |

<a id="chap15"></a>
## Chapitre 15 — Mise à Disposition

---

### Endpoints (`MiseADispositionController`, base `/misesADisposition`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celle
explicitement réannotée.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/misesADisposition/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| GET | `/misesADisposition/decaissements` | `FINANCE_OR_ADMIN` | Liste réservée à l'équipe Finance ou à l'admin système. |
| PATCH | `/misesADisposition/toggle/{id}` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/misesADisposition/decaissements/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste des décaissements filtrée, sans pagination. |

Le module ne gère que des décaissements, alimentés par la synchronisation FunPaiement : aucun endpoint de création, de modification, de suppression ni d'encaissement (retirés le 11/10/2026). Les encours se consultent, s'exportent et se valident depuis le Récapitulatif des États des encours (chapitre 29).

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`).

---

### Guards Angular (`mise-a-disposition.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/Mise-a-disposition/decaissement` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Mises à disposition (Décaissements)" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Commutateur de statut (ENCOURS ↔ REGLE) | `isAdmin` (`authService.isSystemAdmin()`) | `MiseADispositionDecaissementListComponent` |

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / exporter les mises à disposition (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut d'une mise à disposition | `SYSTEM_ADMIN` uniquement |

<a id="chap16"></a>
## Chapitre 16 — Recap Encaissement / Recap Décaissement

---

### Endpoints (`RecapMoyenPaiementController`, base `/recap-moyens-paiement`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes. Module en
lecture seule : aucun endpoint de création, modification ou suppression.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/recap-moyens-paiement/encaissement` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Consultation réservée à l'équipe Finance ou à l'admin système — aligné sur les 5 modules moyens de paiement sources. |
| GET | `/recap-moyens-paiement/decaissement` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/recap-moyens-paiement/encaissement/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF du recap encaissement filtré, sans pagination. |
| GET | `/recap-moyens-paiement/decaissement/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF du recap décaissement filtré, sans pagination. |

---

### Guards Angular (`recap-moyens-paiement.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/recap-moyens-paiement/encaissements` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/recap-moyens-paiement/decaissements` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Recap Encaissement" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Recap Décaissement" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

Aucune — écrans strictement en lecture seule (pas de bouton de suppression, de modification ou de
changement de statut sur ces vues consolidées).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter le Recap Encaissement / Recap Décaissement (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |

<a id="chap17"></a>
## Chapitre 17 — Prélèvement

---

### Endpoints (`PrelevementController`, base `/prelevements`)

Classe annotée `@PreAuthorize(FINANCE_OR_ADMIN)` — s'applique à toutes les méthodes sauf celle
explicitement réannotée.

**Avant le sweep de conformité (11/07/2026), cette classe n'avait aucun `@PreAuthorize`, ni au
niveau classe ni au niveau méthode : l'intégralité de l'API REST du module était accessible à tout
utilisateur authentifié, quel que soit son département.** Corrigé en priorité, même méthode que
Virement International / Mise à Disposition.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/prelevements` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/prelevements/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/prelevements/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/prelevements/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/prelevements/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/prelevements/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| PATCH | `/prelevements/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN` (11/07/2026, aligné sur tous les modules moyens de paiement — décision utilisateur explicite). |
| GET | `/prelevements/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de la liste filtrée (mêmes filtres que `POST /prelevements/search`), sans pagination. |
| GET | `/prelevements/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (22/07/2026) — export PDF de l'encours de prélèvements filtré, sans pagination. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`
(n'était pas affecté par la faille ci-dessus, propre à `PrelevementController`).

---

### Guards Angular (`prelevements.routes.ts`)

**Avant le sweep, aucune route n'avait de guard** — corrigé en même temps que le RBAC backend.

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/prelevements` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/prelevements/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/prelevements/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

**Avant le sweep, l'entrée sidebar n'avait pas de `requiredAuthority`** — visible par tout
utilisateur authentifié, corrigé en même temps.

| Item | `requiredAuthority` |
|---|---|
| "Les Prélèvements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` | `PrelevementListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `PrelevementListComponent` |

**Divergence backend/frontend résolue (11/07/2026)** : le commutateur de statut n'était visible
dans l'UI que pour `SYSTEM_ADMIN` (`isAdmin()`), alors que l'endpoint
`PATCH /prelevements/{id}/toggle-status` autorisait aussi `DEPT_Finance:MEMBER`/`DEPT_Finance:DIRECTOR`
côté backend. Décision utilisateur explicite : au lieu d'ouvrir le commutateur à toute l'équipe
Finance pour aligner Prélèvements sur les autres modules, c'est l'inverse qui a été fait — le
commutateur de statut est désormais réservé à `SYSTEM_ADMIN` dans **tous** les modules moyens de
paiement (Chèques, LCN, Virements, Virement International, Mise à Disposition, Prélèvements), le
comportement déjà en place ici sur Prélèvements devenant la référence commune. Voir
`docs/rbac/cheque.md`, `docs/rbac/lcn.md`, `docs/rbac/virement.md`,
`docs/rbac/virement-international.md`, `docs/rbac/mise-a-disposition.md`.

---

### Génération automatique (`ChargesFixesScheduler`)

Ce composant n'expose aucun endpoint HTTP — il s'exécute uniquement en tâche planifiée
(`@Scheduled`, dernier jour du mois à 07h00), sans intervention utilisateur, et n'est donc soumis à
aucun RBAC. Il appelle directement `IPrelevementService.createPrelevement(...)` côté serveur pour
certaines banques (ex. CNSS). Le même scheduler génère aussi des frais bancaires, des impôts/taxes
et des agios — composant transverse, non spécifique à ce module.

**Anomalie signalée le 16/07/2026, corrigée le 16/07/2026** : `docs/modules/prelevement.md`
affirmait à tort que la saisie d'un prélèvement est "toujours manuelle", ce qui contredisait ce
mécanisme. La documentation fonctionnelle (sections 1, 2 et 4), la note direction
(`docs/direction/prelevement.md`) et ce document ont été alignés sur le comportement réel du code
(saisie manuelle ou génération automatique récurrente via `ChargesFixesScheduler`).

Les définitions de charges lues par ce scheduler sont administrables via
`ChargeFixeConfigController` (base `/charge-fixe-config`, `SYSTEM_ADMIN` uniquement) — documenté dans
`docs/rbac/frais-bancaires.md` (module de rattachement historique du package `chargesFixes`).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un prélèvement (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un prélèvement | `SYSTEM_ADMIN` uniquement |

*Chaque moyen de paiement alimente le module Transactions, qui centralise l'ensemble des écritures.*

<a id="chap18"></a>
## Chapitre 18 — Transactions

Rôle utilisé dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
```

---

### Endpoints — `TransactionController` (base `/transactions`)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 5 endpoints d'un coup (aucune
suppression ni action réservée `SYSTEM_ADMIN` dans ce module).

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/transactions/history` | `FINANCE_OR_ADMIN` (classe) | Historique global, paginé et filtrable. |
| GET | `/transactions/history/export/pdf` | `FINANCE_OR_ADMIN` (classe) | Export PDF de l'historique — mêmes critères que `/transactions/history`, sans pagination. |
| GET | `/transactions/{id}` | `FINANCE_OR_ADMIN` (classe) | Lecture d'une transaction. |
| GET | `/transactions/{banqueId}/transactions` | `FINANCE_OR_ADMIN` (classe) | Transactions agrégées d'une banque. |
| GET | `/transactions/{banqueId}/transactions/export/pdf` | `FINANCE_OR_ADMIN` (classe) | Nouveau (23/07/2026) — export PDF des transactions d'une banque, mêmes critères que l'endpoint ci-dessus, sans pagination. Écran "Rapprochement en cours" du module États des encours. |
| PATCH | `/transactions/{transactionId}` | `FINANCE_OR_ADMIN` (classe) | Modification d'une transaction déjà validée (dates, libellé, observation, TVA recalculée par `TvaResolver`) — le montant, la banque et la date d'émission ne sont pas éditables (absents du DTO). Propagation automatique des champs partagés vers l'entité source (architecture.md, Règle 2). Consommée par les écrans Rapprochement (en cours et historique/détails) / États des encours. **Autorisée aussi quand le rapprochement est `CLOTURE_DEFINITIVE`** (décision HMAYDA, 17/10/2026 — correction d'erreurs découvertes lors de la vérification des agios) : aucun garde de statut, mêmes droits `FINANCE_OR_ADMIN`. Si la date de valeur change, recalcul immédiat des périodes d'agios ouvertes ; la réponse porte `avertissements` pour chaque période d'agios clôturée non recalculée. |

**Corrigé pendant ce sweep** : le controller n'avait auparavant aucun `@PreAuthorize` — seule la
règle globale `anyRequest().authenticated()` (`SecurityConfig`) s'appliquait, ce qui permettait à
tout utilisateur connecté, quel que soit son département, de consulter et de modifier n'importe
quelle transaction. Niveau retenu (`FINANCE_OR_ADMIN`) choisi pour s'aligner sur l'usage réel du
PATCH (consommé par des écrans Finance) plutôt que sur l'ancienne restriction frontend
`SYSTEM_ADMIN` de la page "Historique des transactions" (elle-même corrigée en conséquence — voir
ci-dessous).

---

### Endpoint hors module — `DELETE /rapprochement/annuler/{transactionId}`

Supprime réellement une transaction (annulation), mais est déclaré dans `ValidationController`
(module `validation`), pas dans `TransactionController`. Protégé par `FINANCE_OR_ADMIN` au niveau
classe de `ValidationController` — **pas** `SYSTEM_ADMIN`, en dérogation assumée à la règle
transverse `conventions/security.md` ("toute suppression est réservée `SYSTEM_ADMIN`").

**Divergence confirmée intentionnelle par HMAYDA (16/07/2026)** : l'équipe Finance doit pouvoir
annuler elle-même une transaction erronée dans un rapprochement `EN_COURS` ou
`CLOTURE_PROVISOIRE` — par exemple une erreur de saisie, ou une correction après réception du
relevé définitif de la banque — sans dépendre d'un `SYSTEM_ADMIN` pour chaque annulation. Le
garde-fou réel n'est pas le rôle mais le **statut du rapprochement** : l'opération est bloquée dès
que celui-ci est `CLOTURE_DEFINITIVE`, quel que soit le rôle de l'appelant.

Garde déjà implémentée et vérifiée — `ReverseTransactionService.annulerTransaction` :

```java
if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
    throw new ValidationException(
            "Impossible d'annuler la transaction " + transactionId
            + " : le rapprochement est définitivement clôturé.");
}
```

---

### Guards Angular

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/historique-des-transactions` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |

**Corrigé pendant ce sweep** : la route utilisait `systemAdminGuard` (réservait la page au seul
admin système), incohérent avec le niveau d'accès retenu côté backend (`FINANCE_OR_ADMIN`) et avec
l'usage réel de la modification de transaction (déjà accessible à Finance depuis d'autres écrans).
Remplacé par `departmentGuard('Finance')`.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Historique des transactions" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

**Corrigé pendant ce sweep** : l'entrée portait auparavant `requiredAuthority: 'SYSTEM_ADMIN'`,
incohérente avec le guard de route corrigé ci-dessus.

---

### Actions conditionnées dans l'UI

Aucune — le module n'expose ni suppression ni action réservée à un sous-ensemble de Finance. La
modification (dates, TVA) et l'export PDF sont disponibles à tout utilisateur ayant accès à l'écran
concerné (bouton "Exporter PDF" — pas de garde supplémentaire au-delà de l'accès à l'écran).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter l'historique et le détail d'une transaction | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter l'historique en PDF | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Modifier une transaction (dates, TVA) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Annuler une transaction (`DELETE /rapprochement/annuler/{id}`) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — dérogation assumée à `security.md`, bloquée si le rapprochement est `CLOTURE_DEFINITIVE` (voir section dédiée ci-dessus) |

*Les transactions génèrent des charges associées : frais bancaires, agios, impôts et taxes, commissions.*

<a id="chap19"></a>
## Chapitre 19 — Frais Bancaires

---

### Endpoints (`FraisBancaireController`, base `/frais-bancaire`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/frais-bancaire` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/frais-bancaire/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/frais-bancaire/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| GET | `/frais-bancaire/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste — mêmes critères que `/frais-bancaire/search`, sans pagination. |
| PUT | `/frais-bancaire/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/frais-bancaire/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/frais-bancaire/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. |
| GET | `/frais-bancaire/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours de frais bancaires d'une banque, mêmes critères que l'endpoint ci-dessus, sans pagination. Écran "Décaissements / Frais Bancaires" du module États des encours. |
| PATCH | `/frais-bancaire/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Chèques, LCN, Virements, Impôts et Taxes, Impayées, Commissions). |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.

`ChargesFixesScheduler` (génération automatique mensuelle) appelle directement
`IFraisBancaireService.createFraisBancaire(...)` côté serveur — hors périmètre HTTP, aucun
`@PreAuthorize` applicable. Depuis le 16/07/2026, ce scheduler lit ses définitions de charges via
`IChargeFixeConfigService.findAllEntities()` (voir section suivante) au lieu du registre Java codé en
dur auparavant (`ChargesFixesRegistry`, supprimé).

---

### Endpoints (`ChargeFixeConfigController`, base `/charge-fixe-config`)

Administration des charges fixes bancaires récurrentes (frais bancaires, impôts/taxes, agios,
prélèvements), partagées par les modules Frais bancaires, Impôts et Taxes, Agios et Prélèvements —
documentées ici car le package `chargesFixes` (scheduler + configuration) est rattaché
historiquement à ce module. Remplace `ChargesFixesRegistry` (Java codé en dur), conformément à
`conventions/configuration.md`.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/charge-fixe-config` | `SYSTEM_ADMIN` (classe) | Consultation des charges réservée à l'admin système. |
| POST | `/charge-fixe-config` | `SYSTEM_ADMIN` (classe) | Création d'une charge réservée à l'admin système. |
| PUT | `/charge-fixe-config/{id}` | `SYSTEM_ADMIN` (classe) | Modification d'une charge réservée à l'admin système. |
| DELETE | `/charge-fixe-config/{id}` | `SYSTEM_ADMIN` (classe) | Suppression d'une charge réservée à l'admin système. |

La route Angular `/portail/admin/charges-fixes-config` n'a pas de guard propre — elle hérite de
`systemAdminGuard` posé au niveau parent (`portail/admin`), comme toutes les routes du module
Administration. Sidebar : item "Charges fixes bancaires" sous le groupe "Administration"
(`requiredAuthority: SYSTEM_ADMIN` hérité du groupe).

---

### Guards Angular (`frais-bancaire.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/frais-bancaire` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/frais-bancaire/new` | `departmentGuard('Finance')` | Idem (hérité du guard posé sur `portail.routes.ts`, la route enfant elle-même n'a pas de guard propre). |
| `/portail/frais-bancaire/edit/:id` | `departmentGuard('Finance')` | Idem. |

Guard ajouté lors du sweep de conformité — absent auparavant (aucune restriction de route avant ce
changement).

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Frais bancaires" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

`requiredAuthority` ajouté lors du sweep de conformité — absent auparavant (item visible à tout
utilisateur connecté).

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `FraisBancaireListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `FraisBancaireListComponent` |

Aucune divergence backend/frontend : les deux actions ci-dessus sont réservées à `SYSTEM_ADMIN` des
deux côtés, cohérent avec l'endpoint `PATCH /frais-bancaire/{id}/toggle-status` et `DELETE
/frais-bancaire/{id}`.

Le bouton "Exporter PDF" (`FraisBancaireListComponent`) n'a pas de garde propre au-delà de l'accès à
l'écran — pas de restriction supplémentaire au-delà de `departmentGuard('Finance')`.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier un frais bancaire (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF la liste des frais bancaires | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un frais bancaire | `SYSTEM_ADMIN` uniquement |
| Consulter / créer / modifier / supprimer une charge fixe bancaire récurrente (API + écran) | `SYSTEM_ADMIN` uniquement |

<a id="chap20"></a>
## Chapitre 20 — Agios

Rôles utilisés dans ce document :

```java
private static final String FINANCE_OR_ADMIN =
        "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
```

---

### Endpoints — `AgiosController` (base `/agios`) — Agios ponctuel

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/agios` | `FINANCE_OR_ADMIN` | Création d'un agios manuel. |
| GET | `/agios/{id}` | `FINANCE_OR_ADMIN` | Lecture par ID. |
| POST | `/agios/search` | `FINANCE_OR_ADMIN` | Recherche paginée. |
| PUT | `/agios/{id}` | `FINANCE_OR_ADMIN` | Modification — bloquée en service si l'agios est déjà `PAYE`. |
| DELETE | `/agios/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`) : suppression toujours `SYSTEM_ADMIN`. Refusée en plus si l'agios n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/agios/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Agios en attente de paiement (vue Encours) pour une banque. |
| GET | `/agios/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste des agios ponctuels — mêmes critères que la recherche, sans pagination. |
| GET | `/agios/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF des agios en attente de paiement d'une banque. |
| PATCH | `/agios/{id}/toggle-status` | `SYSTEM_ADMIN` | Bascule manuelle `PREVU` ↔ `PAYE` sans passer par le rapprochement — action sensible réservée à l'admin système. |

---

### Endpoints — `AgiosCalculController` (base `/agios/calcul`) — Calcul automatique

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/agios/calcul/types` | `FINANCE_OR_ADMIN` | Référentiel des types d'agios actifs. |
| GET | `/agios/calcul/trimestres` | `FINANCE_OR_ADMIN` | Recherche paginée des périodes (trimestres ou mois selon le type). |
| GET | `/agios/calcul/detail` | `FINANCE_OR_ADMIN` | Détail journalier paginé d'une période. |
| GET | `/agios/calcul/detail/aggregates` | `FINANCE_OR_ADMIN` | Agrégats journaliers d'une période. |
| GET | `/agios/calcul/detail/marches` | `FINANCE_OR_ADMIN` | Marchés disponibles pour le filtre marché du détail — uniquement ceux effectivement présents dans le détail journalier de la banque/type/période consultés, jamais le référentiel marché complet. |
| GET | `/agios/calcul/trimestres/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF des périodes (trimestres/mois) — mêmes critères que la recherche paginée, sans pagination. |
| GET | `/agios/calcul/detail/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF du détail journalier d'une période — mêmes critères que la recherche paginée, sans pagination. |
| POST | `/agios/calcul/recalculer` | `FINANCE_OR_ADMIN` | Recalcul manuel du trimestre/mois courant, toutes banques. |
| POST | `/agios/calcul/banque/{banqueId}/recalculer/{trim}/{annee}` | `FINANCE_OR_ADMIN` | Recalcul manuel d'une période précise pour une banque. |
| POST | `/agios/calcul/banque/{banqueId}/type/{type}/cloturer/{trim}/{annee}` | `FINANCE_OR_ADMIN` | Clôture définitive d'une période — génère l'agios définitif si le total TTC > 0. |

**Note** : les dix endpoints du calcul automatique sont ouverts à `FINANCE_OR_ADMIN` au niveau backend, y compris le recalcul ciblé et la clôture. Le frontend restreint volontairement ces deux dernières actions au profil `SYSTEM_ADMIN` — voir "Divergence assumée" ci-dessous.

---

### Guards Angular (`portail.routes.ts` / `agios.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/agios` (parent — liste, `new`, `edit/:id`, `calcul`, `calcul/detail/:banqueId/:type/:trim/:annee`) | `departmentGuard('Finance')` (posé une seule fois au niveau du parent dans `portail.routes.ts`) | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |

Toutes les routes du module (Agios ponctuel et Calcul automatique) héritent du même guard — aucun guard supplémentaire n'est posé sur la sous-arborescence `calcul`.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Agios" (section) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |
| "Liste des Agios" | idem |
| "Calcul des Agios" | idem |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (agios ponctuel) | `isSystemAdmin()` | `AgiosListComponent` |
| Bouton bascule de statut (toggle PREVU ↔ PAYE) | `isSystemAdmin()` | `AgiosListComponent` |
| Bouton "Recalculer" (ligne, période OUVERT) | `trim.statut === OUVERT` — visible à toute l'équipe Finance (route sous `departmentGuard('Finance')`) | `AgiosCalculDashboardComponent` |
| Bouton "Clôturer" (ligne, période OUVERT) | `isAdmin()` | `AgiosCalculDashboardComponent` |
| Bouton "Clôturer" (vue détail) | `isAdmin() && trimestreInfo.statut === OUVERT` | `AgiosCalculDetailComponent` |
| Bouton "Recalculer tout" (global) | Aucune condition — visible à toute l'équipe Finance | `AgiosCalculDashboardComponent` |

---

### Divergence assumée — clôture restreinte à l'admin en UI

Le backend autorise `FINANCE_OR_ADMIN` sur les trois actions d'écriture du calcul automatique (`recalculer`, `recalculer/{trim}/{annee}`, `cloturer/{trim}/{annee}`). Le frontend, lui, distingue :

- **"Recalculer tout"** et **"Recalculer"** une période précise (actions idempotentes, sans effet financier direct — elles ne font que rejouer le calcul) : visibles à toute l'équipe Finance (décision du 12/10/2026 pour le recalcul ciblé), cohérent avec le backend.
- **"Clôturer"** (action irréversible, qui génère un agios définitif à payer) : bouton masqué sauf pour `SYSTEM_ADMIN`, restriction plus stricte que le backend.

Cette divergence est intentionnelle : elle restreint en interface une action financière sensible et irréversible à l'administrateur système, tout en laissant le backend ouvert à `FINANCE_OR_ADMIN` par cohérence avec le reste du module. Une restriction UI plus stricte que le backend est acceptable ; l'inverse ne le serait pas.

---

### Génération automatique (`ChargesFixesScheduler`)

Ce composant n'expose aucun endpoint HTTP — il s'exécute uniquement en tâche planifiée
(`@Scheduled`, dernier jour du mois à 07h00), sans intervention utilisateur, et n'est donc soumis à
aucun RBAC. Il génère automatiquement, entre autres, des agios récurrents pour certaines banques
(voir `docs/modules/agios.md`, section 10). Le même scheduler génère aussi des frais bancaires, des
impôts/taxes et des prélèvements — composant transverse, non spécifique à ce module.

Les définitions de charges lues par ce scheduler sont administrables via
`ChargeFixeConfigController` (base `/charge-fixe-config`, `SYSTEM_ADMIN` uniquement) — documenté dans
`docs/rbac/frais-bancaires.md` (module de rattachement historique du package `chargesFixes`).

---

### Résumé

| Action | Qui peut faire |
|--------|----------------|
| Consulter les agios (ponctuel et calcul automatique) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF (agios ponctuel, encours, périodes, détail journalier) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Créer / modifier un agios ponctuel | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Valider un agios (rapprochement) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Déclencher un recalcul global ("Recalculer tout") | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Supprimer un agios ponctuel | `SYSTEM_ADMIN` uniquement |
| Basculer manuellement le statut d'un agios ponctuel | `SYSTEM_ADMIN` uniquement |
| Recalculer une période précise (UI) | `SYSTEM_ADMIN` uniquement (backend autorise aussi Finance, non exposé en UI) |
| Clôturer une période (UI) | `SYSTEM_ADMIN` uniquement (backend autorise aussi Finance, non exposé en UI) |

<a id="chap21"></a>
## Chapitre 21 — Impôts et Taxes

---

### Endpoints (`ImpotEtTaxeController`, base `/impots-taxes`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/impots-taxes` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/impots-taxes/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/impots-taxes/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/impots-taxes/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/impots-taxes/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/impots-taxes/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem. |
| GET | `/impots-taxes/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste filtrée (mêmes filtres que `POST /impots-taxes/search`, ignore la pagination). Réutilisé tel quel par l'écran "TVA sur cautions" du module Cautions (filtre `motif=TVA_COMMISSION_CAUTION`, voir `docs/rbac/caution.md`). Bouton "Exporter PDF" ajouté à l'écran liste (`ImpotEtTaxeListComponent`) le 23/07/2026 — l'endpoint existait déjà mais n'était pas encore appelé depuis cet écran. |
| GET | `/impots-taxes/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours d'une banque (mêmes filtres que `GET /impots-taxes/encours/{banqueId}`, ignore la pagination). Bouton "Exporter PDF" sur l'écran encours (`EncoursImpotsEtTaxesComponent`, États des encours → Décaissement). |
| PATCH | `/impots-taxes/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Chèques, LCN, Virements, Virement International, Mise à Disposition, Prélèvements). |
| POST | `/impots-taxes/fusionner` | `FINANCE_OR_ADMIN` | Fusion de TVA réservée à l'équipe Finance ou à l'admin système. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.

---

### Guards Angular (`impot-et-taxe.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/impots-taxes` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/impots-taxes/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/impots-taxes/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Impôts et Taxes" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` | `ImpotEtTaxeListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `ImpotEtTaxeListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `ImpotEtTaxeListComponent` |
| Bouton "Exporter PDF" (encours) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `EncoursImpotsEtTaxesComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /impots-taxes/{id}/toggle-status` et
`DELETE /impots-taxes/{id}`. Les boutons "Exporter PDF" (23/07/2026) ne portent aucune condition
d'affichage supplémentaire côté frontend — visibles pour tout utilisateur Finance ou admin ayant
accès à l'écran, cohérent avec `FINANCE_OR_ADMIN` côté backend sur les deux endpoints
`GET /impots-taxes/export/pdf` et `GET /impots-taxes/encours/{banqueId}/export/pdf`.

La fusion de TVA n'a pas d'écran dédié dans ce module : elle est déclenchée depuis l'écran
"TVA sur cautions" du module Cautions (`TvaCautionListComponent`), lui-même soumis au RBAC du module
Cautions (voir `docs/rbac/caution.md`). Le bouton "Exporter PDF" de ce même écran appelle également
ce module (`GET /impots-taxes/export/pdf`) — aucune divergence de RBAC, le bouton est visible pour
tout utilisateur ayant accès à l'écran (`FINANCE_OR_ADMIN`).

---

### Génération automatique (`ChargesFixesScheduler`)

Ce composant n'expose aucun endpoint HTTP — il s'exécute uniquement en tâche planifiée
(`@Scheduled`), sans intervention utilisateur, et n'est donc soumis à aucun RBAC. Il génère
automatiquement, entre autres, des impôts/taxes pour certaines banques (voir
`docs/modules/impots-taxes.md`, section 4). Le même scheduler génère aussi des frais bancaires, des
agios et des prélèvements — composant transverse, non spécifique à ce module.

Les définitions de charges lues par ce scheduler sont administrables via
`ChargeFixeConfigController` (base `/charge-fixe-config`, `SYSTEM_ADMIN` uniquement) — documenté dans
`docs/rbac/frais-bancaires.md` (module de rattachement historique du package `chargesFixes`).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier / fusionner un impôt/taxe (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer un impôt/taxe | `SYSTEM_ADMIN` uniquement |

<a id="chap22"></a>
## Chapitre 22 — Commission

---

### Endpoints (`CommissionController`, base `/commissions`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/commissions` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/commissions/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/commissions/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/commissions/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/commissions/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. Refusée en plus si la commission n'est plus au statut PREVU (règle métier, `docs/modules/commission.md` — harmonisée le 14/07/2026 avec Impayées/Restitution/Impôts et Taxes). |
| GET | `/commissions/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. |
| GET | `/commissions/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste filtrée (mêmes filtres que `POST /commissions/search`, ignore la pagination). Bouton "Exporter PDF" sur l'écran liste (`CommissionListComponent`). |
| GET | `/commissions/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours d'une banque (mêmes filtres que `GET /commissions/encours/{banqueId}`, ignore la pagination). Bouton "Exporter PDF" sur l'écran encours (`EncoursCommissionsComponent`, États des encours → Décaissement). |
| PATCH | `/commissions/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Chèques, LCN, Virements, Virement International, Mise à Disposition, Prélèvements, Impôts et Taxes). |
| POST | `/commissions/fusionner` | `FINANCE_OR_ADMIN` | Fusion de commissions sur cautions réservée à l'équipe Finance ou à l'admin système. |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`
(`FINANCE_OR_ADMIN`).

### Endpoints (`CommissionConfigController`, base `/commission-config`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/commission-config` | `SYSTEM_ADMIN` (classe) | Consultation des règles réservée à l'admin système. |
| POST | `/commission-config` | `SYSTEM_ADMIN` (classe) | Création d'une règle réservée à l'admin système. |
| PUT | `/commission-config/{id}` | `SYSTEM_ADMIN` (classe) | Modification d'une règle réservée à l'admin système. |
| DELETE | `/commission-config/{id}` | `SYSTEM_ADMIN` (classe) | Suppression d'une règle réservée à l'admin système. |

---

### Guards Angular (`commission.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/commissions` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/commissions/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/commissions/edit/:id` | `departmentGuard('Finance')` | Idem. |

La route `/portail/admin/commission-config` n'a pas de guard propre — elle hérite de
`systemAdminGuard` posé au niveau parent (`portail/admin`, voir `portail.routes.ts`), comme toutes
les routes du module Administration.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Commissions" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |
| "Règles de commission" (sous-item Administration) | Hérite de `SYSTEM_ADMIN` posé sur le groupe "Administration" |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `CommissionListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `CommissionListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `CommissionListComponent` |
| Bouton "Exporter PDF" (encours) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `EncoursCommissionsComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /commissions/{id}/toggle-status` et
`DELETE /commissions/{id}`. Les boutons "Exporter PDF" (23/07/2026) ne portent aucune condition
d'affichage supplémentaire côté frontend — visibles pour tout utilisateur Finance ou admin ayant
accès à l'écran, cohérent avec `FINANCE_OR_ADMIN` côté backend sur les deux endpoints
`GET /commissions/export/pdf` et `GET /commissions/encours/{banqueId}/export/pdf`.

L'écran d'administration des règles (`CommissionConfigListComponent`) n'a pas de conditions
d'affichage internes supplémentaires — l'ensemble de la page n'est accessible qu'aux `SYSTEM_ADMIN`
via le guard parent.

---

### Génération automatique (sans endpoint HTTP dédié)

Les commissions générées automatiquement (`CommissionService.generateCommissionIfNeeded(...)` pour
Virement / LCN / Mise à disposition, `creerCommissionCaution` et `creerCommissionSurImpotEtTaxe`
pour Cautions et Impôts et Taxes) ne transitent par aucun endpoint HTTP dédié — elles sont
déclenchées en interne, via les façades `ICommissionService`/`ICommissionConfigService`, lors de la
validation d'une opération dans un autre module (Virements, LCN, Mise à disposition, Cautions,
Impôts et Taxes). Le RBAC applicable est celui de l'action déclenchante dans le module source (voir
`docs/rbac/<module-source>.md`) — aucun RBAC propre à ce mécanisme.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier / fusionner / exporter une commission (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer une commission | `SYSTEM_ADMIN` uniquement |
| Consulter / créer / modifier / supprimer une règle de commission | `SYSTEM_ADMIN` uniquement |

*Les anomalies suivantes interviennent lorsqu'une transaction ou une charge présente un incident.*

<a id="chap23"></a>
## Chapitre 23 — Impayés

---

### Endpoints (`ImpayeeController`, base `/impayees`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/impayees` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/impayees/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/impayees/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/impayees/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/impayees/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/impayees/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. |
| GET | `/impayees/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de la liste filtrée (mêmes filtres que `POST /impayees/search`, ignore la pagination). Bouton "Exporter PDF" sur l'écran liste (`ImpayeeListComponent`). Aucun export n'existait sur ce module avant cette date. |
| GET | `/impayees/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours d'une banque (mêmes filtres que `GET /impayees/encours/{banqueId}`, `type` inclus, ignore la pagination). Bouton "Exporter PDF" sur les deux écrans d'encours (`DecaissementEncoursImpayeeComponent` et `EncaissementEncoursImpayeeComponent`, États des encours), chacun fixant `type` à sa direction. |
| PATCH | `/impayees/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Chèques, LCN, Virements, Virement International, Mise à Disposition, Prélèvements, Impôts et Taxes, Commissions). |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.

---

### Guards Angular (`impayees.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/impayees` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/impayees/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/impayees/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Impayées" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `ImpayeeListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `ImpayeeListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `ImpayeeListComponent` |
| Bouton "Exporter PDF" (encours décaissement) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `DecaissementEncoursImpayeeComponent` |
| Bouton "Exporter PDF" (encours encaissement) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `EncaissementEncoursImpayeeComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /impayees/{id}/toggle-status` et
`DELETE /impayees/{id}`. Les boutons "Exporter PDF" (23/07/2026) ne portent aucune condition
d'affichage supplémentaire côté frontend — visibles pour tout utilisateur Finance ou admin ayant
accès à l'écran, cohérent avec `FINANCE_OR_ADMIN` côté backend sur les deux endpoints
`GET /impayees/export/pdf` et `GET /impayees/encours/{banqueId}/export/pdf`.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier une impayée (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF (liste ou encours) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer une impayée | `SYSTEM_ADMIN` uniquement |

<a id="chap24"></a>
## Chapitre 24 — Opérations Erronées

---

### Endpoints (`OperationErroneController`, base `/operation-errone`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/operation-errone` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/operation-errone/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/operation-errone/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/operation-errone/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/operation-errone/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. Refusée en plus si l'opération erronée n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/operation-errone/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. |
| PATCH | `/operation-errone/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Impayées, Restitution, Impôts et Taxes, Commissions, etc.). |
| GET | `/operation-errone/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de la liste filtrée (mêmes filtres que `POST /operation-errone/search`, ignore la pagination). Bouton "Exporter PDF" sur l'écran liste (`OperationErroneListComponent`). Aucun export n'existait sur ce module avant cette date. |
| GET | `/operation-errone/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours d'une banque (mêmes filtres que `GET /operation-errone/encours/{banqueId}`, ignore la pagination). Bouton "Exporter PDF" sur l'écran encours (`EncoursOperationErroneComponent`, États des encours → Décaissement). |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.

---

### Guards Angular (`operation-errone.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/operations/errones` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/operations/errones/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/operations/errones/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Opérations Erronées" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `OperationErroneListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `OperationErroneListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `OperationErroneListComponent` |
| Bouton "Exporter PDF" (encours) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `EncoursOperationErroneComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /operation-errone/{id}/toggle-status`
et `DELETE /operation-errone/{id}`. Les boutons "Exporter PDF" (23/07/2026) ne portent aucune
condition d'affichage supplémentaire côté frontend — visibles pour tout utilisateur Finance ou admin
ayant accès à l'écran, cohérent avec `FINANCE_OR_ADMIN` côté backend sur les deux endpoints
d'export.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier une opération erronée (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer une opération erronée | `SYSTEM_ADMIN` uniquement |

<a id="chap25"></a>
## Chapitre 25 — Provision

---

### Endpoints (`ProvisionController`, base `/provision`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/provision` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/provision/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/provision/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/provision/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/provision/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. Refusée en plus si la provision n'est plus au statut PREVU (règle métier générale "PAYE non supprimable", `conventions/architecture.md`). |
| GET | `/provision/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. Filtré par `sens` côté frontend (deux écrans, décaissements et encaissements — voir `docs/rbac/README.md`, chapitre États des encours). |
| PATCH | `/provision/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Impayées, Restitution, Opérations Erronées, etc.). |
| GET | `/provision/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de la liste filtrée (mêmes filtres que `POST /provision/search`, ignore la pagination). Bouton "Exporter PDF" sur l'écran liste (`ProvisionListComponent`). |
| GET | `/provision/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Export PDF de l'encours d'une banque (mêmes filtres que `GET /provision/encours/{banqueId}`, ignore la pagination). Bouton "Exporter PDF" sur chacun des deux écrans d'encours (États des encours → Décaissement / Encaissement → Provisions). |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.
Le sens de la transaction générée est déterminé côté backend par `provision.getSens()`, jamais
transmis par l'appelant.

---

### Guards Angular (`provision.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/provisions` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/provisions/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/provisions/edit/:id` | `departmentGuard('Finance')` | Idem. |

Les deux écrans d'encours (`provisions-decaissements`, `provisions-encaissements`) sont des routes
enfants de `/portail/etats-des-encours/:banqueId/:rapprochementId`, protégées par le
`departmentGuard('Finance')` déjà posé au niveau du parent — voir chapitre États des encours et
Récapitulatif.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Provisions" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `ProvisionListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `ProvisionListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `ProvisionListComponent` |
| Bouton "Exporter PDF" (encours) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `ProvisionEncoursDecaissementComponent` / `ProvisionEncoursEncaissementComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /provision/{id}/toggle-status` et
`DELETE /provision/{id}`.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier une provision (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer une provision | `SYSTEM_ADMIN` uniquement |

<a id="chap26"></a>
## Chapitre 26 — Restitution

---

### Endpoints (`RestitutionController`, base `/restitutions`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/restitutions` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Création réservée à l'équipe Finance ou à l'admin système. |
| GET | `/restitutions/{id}` | `FINANCE_OR_ADMIN` | Consultation réservée à l'équipe Finance ou à l'admin système. |
| POST | `/restitutions/search` | `FINANCE_OR_ADMIN` | Recherche réservée à l'équipe Finance ou à l'admin système. |
| PUT | `/restitutions/{id}` | `FINANCE_OR_ADMIN` | Modification réservée à l'équipe Finance ou à l'admin système. |
| DELETE | `/restitutions/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. Refusée en plus si la restitution n'est plus au statut PREVU (règle métier, `docs/modules/restitution.md`). |
| GET | `/restitutions/encours/{banqueId}` | `FINANCE_OR_ADMIN` | Idem consultation. |
| PATCH | `/restitutions/{id}/toggle-status` | `SYSTEM_ADMIN` | Changement de statut réservé à `SYSTEM_ADMIN`, aligné sur tous les modules moyens de paiement / charges (Impayées, Impôts et Taxes, Commissions, etc.). |
| GET | `/restitutions/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de la liste filtrée (mêmes filtres que `POST /restitutions/search`, ignore la pagination). Bouton "Exporter PDF" sur l'écran liste (`RestitutionListComponent`). Aucun export n'existait sur ce module avant cette date. |
| GET | `/restitutions/encours/{banqueId}/export/pdf` | `FINANCE_OR_ADMIN` | Nouveau (23/07/2026) — export PDF de l'encours d'une banque (mêmes filtres que `GET /restitutions/encours/{banqueId}`, ignore la pagination). Bouton "Exporter PDF" sur l'écran encours (`EncoursRestitutionComponent`, États des encours → Encaissement). |

Endpoint de validation (rapprochement) : voir Chapitre 28 — Validation (`ValidationController`,
`POST /rapprochement/process`) — protégé par le `@PreAuthorize` de classe de `ValidationController`.

---

### Guards Angular (`restitution.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/restitutions` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/restitutions/new` | `departmentGuard('Finance')` | Idem. |
| `/portail/restitutions/edit/:id` | `departmentGuard('Finance')` | Idem. |

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Restitutions" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` |

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" | `isAdmin()` + désactivé si `status === PAYE` | `RestitutionListComponent` |
| Commutateur de statut (PREVU ↔ PAYE) | `isAdmin()` | `RestitutionListComponent` |
| Bouton "Exporter PDF" (liste) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `RestitutionListComponent` |
| Bouton "Exporter PDF" (encours) | Aucune condition — accès écran seul (`FINANCE_OR_ADMIN`) | `EncoursRestitutionComponent` |

Aucune divergence backend/frontend : les deux premières actions ci-dessus sont réservées à
`SYSTEM_ADMIN` des deux côtés, cohérent avec l'endpoint `PATCH /restitutions/{id}/toggle-status` et
`DELETE /restitutions/{id}`. Les boutons "Exporter PDF" (23/07/2026) ne portent aucune condition
d'affichage supplémentaire côté frontend — visibles pour tout utilisateur Finance ou admin ayant
accès à l'écran, cohérent avec `FINANCE_OR_ADMIN` côté backend sur les deux endpoints d'export.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / créer / modifier une restitution (API + écran) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Basculer le statut / supprimer une restitution | `SYSTEM_ADMIN` uniquement |

*Le Rapprochement (et sa validation) confronte l'ensemble de ces écritures au relevé bancaire.*

<a id="chap27"></a>
## Chapitre 27 — Rapprochement

Rôle utilisé dans ce document :

```java
static final String FINANCE_OR_ADMIN =
        "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
```

---

### Endpoints — `RapprochementController` (base `/rapprochement`)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 6 endpoints d'un coup, y
compris la clôture définitive (irréversible).

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/rapprochement/banque/{banqueId}/actifs` | `FINANCE_OR_ADMIN` (classe) | Snapshot des rapprochements actifs (En cours + éventuelle Clôture provisoire) d'une banque. |
| POST | `/rapprochement/cloturer` | `FINANCE_OR_ADMIN` (classe) | Clôture provisoire — action Finance courante, pas réservée à l'admin système. |
| POST | `/rapprochement/{id}/cloturer-definitivement` | `FINANCE_OR_ADMIN` (classe) | Clôture définitive — irréversible, mais le garde-fou métier réel est l'enchaînement strict des statuts (refusé si le rapprochement n'est pas en Clôture provisoire), pas une restriction de rôle. |
| GET | `/rapprochement/{id}` | `FINANCE_OR_ADMIN` (classe) | Détail d'un rapprochement, transactions paginées. |
| GET | `/rapprochement` | `FINANCE_OR_ADMIN` (classe) | Recherche paginée de l'historique des rapprochements. |
| GET | `/rapprochement/{id}/report/pdf` | `FINANCE_OR_ADMIN` (classe) | Téléchargement du rapport PDF (variante Finance ou Comptabilité). Filtrable (23/07/2026) : accepte les mêmes critères que `GET /rapprochement/{id}` (libellé, observation, montant, sources, dates d'opération) pour ne transcrire que les transactions correspondantes ; sans filtre, comportement historique inchangé (toutes les transactions du rapprochement). Bouton "Exporter PDF" ajouté sur l'écran détail (`DetailsRapprochementComponent`), en plus du bouton existant sur l'écran historique (`HistoriqueDesRapprochementComponent`, jamais filtré car cet écran ne filtre pas les transactions). |

**Corrigé pendant ce sweep** : le controller n'avait auparavant aucun `@PreAuthorize` — seule la
règle globale `anyRequest().authenticated()` (`SecurityConfig`) s'appliquait, ce qui permettait à
tout utilisateur connecté, quel que soit son département, de consulter et de clôturer n'importe
quel rapprochement, y compris définitivement. Niveau retenu (`FINANCE_OR_ADMIN`, uniforme sur les 6
endpoints) confirmé par HMAYDA — même principe que l'annulation de transaction documentée dans
`docs/rbac/transactions.md` : le rôle Finance doit pouvoir piloter lui-même le cycle de clôture
mensuel, la protection réelle étant portée par les transitions de statut.

---

### Guards Angular

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/rapprochement/details/:id` | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. |
| `/portail/rapprochement/historique` | `departmentGuard('Finance')` | idem |

**Corrigé pendant ce sweep** : les deux routes n'avaient auparavant aucun guard — accessibles à
tout utilisateur authentifié, quel que soit son département, incohérent avec le niveau d'accès
retenu côté backend.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Historique des Rapprochements" | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) |

**Corrigé pendant ce sweep** : l'entrée n'avait auparavant aucun `requiredAuthority`, incohérente
avec le guard de route corrigé ci-dessus.

---

### Actions conditionnées dans l'UI

Aucune — le module n'expose aucune action réservée à un sous-ensemble de Finance ; la clôture
provisoire, la clôture définitive et le téléchargement des rapports (filtré ou non) sont disponibles
à tout utilisateur ayant accès à l'écran concerné.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter le snapshot des rapprochements actifs d'une banque | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Consulter l'historique et le détail d'un rapprochement | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Clôturer provisoirement | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Clôturer définitivement (irréversible) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — garde-fou réel = enchaînement des statuts, pas le rôle |
| Télécharger le rapport PDF | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |

<a id="chap28"></a>
## Chapitre 28 — Validation

Rôle utilisé dans ce document :

```java
static final String FINANCE_OR_ADMIN =
        "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
```

---

### Endpoints — `ValidationController` (base `/rapprochement`)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 2 endpoints d'un coup, y
compris l'annulation de transaction (voir justification dans `docs/rbac/transactions.md`, section
"Endpoint hors module").

| Méthode | URL | Module source délégué |
|---|---|---|
| POST | `/rapprochement/process` | Point d'entrée unique, toutes sources confondues (validation d'une seule opération ou d'un lot — dispatch vers le `BatchValidationHandler` de la source, tout-ou-rien) |
| DELETE | `/rapprochement/annuler/{transactionId}` | Annulation de transaction (`ReverseTransactionService`) — documenté en détail dans `docs/rbac/transactions.md` |

**19/09/2026 — Fusion complète validation unitaire + lot (architecture.md, Règle 1).** Les ~22
endpoints `/process/{source}` (un par module source, plus `/process/echeance-leasing` et
`/process/echeance-cmt`) et l'ancien `/process/lot` sont supprimés au profit du seul
`POST /rapprochement/process` : valider une opération n'est qu'un cas particulier de valider un lot
de taille 1 (`ids` à un élément). `BatchValidationRequestDto` porte désormais aussi `montantTva`,
`observation` et `libelle` (n'autorisés que pour `ids` à un seul élément). Chaque
`XxxValidationService` ne conserve qu'une seule méthode de validation par source ; le dispatch reste
entièrement piloté par le registre de `BatchValidationHandler` (aucun changement pour ce mécanisme).
`FINANCE_OR_ADMIN` reste la même restriction qu'avant sur le nouvel endpoint unique. Un handler a été
ajouté à cette occasion : `LcnEscompteBatchValidationHandler` (`TransactionSource.LCN_ESCOMPTE`),
qui n'avait jamais eu de handler de lot (seulement une validation unitaire via l'ancien endpoint
dédié) — sans cet ajout, la fusion aurait cassé la validation des LCN d'escompte.

Cette entrée remplace la précédente ("19/09/2026 — Fusion des endpoints à deux sens", qui avait
réduit Mise à disposition/Virement/Chèque/LCN/Virement international de deux endpoints à un chacun)
— entièrement absorbée par cette fusion plus large.

---

### Guards Angular

Aucun — ce module n'a pas d'écran dédié. Les endpoints sont consommés depuis les formulaires de
saisie de chaque module source (Leasing, CMT, moyens de paiement, etc.), chacun protégé par son
propre guard (voir le `docs/rbac/<module-source>.md` correspondant).

---

### Visibilité sidebar

Aucune — pas d'entrée sidebar propre à ce module.

---

### Actions conditionnées dans l'UI

Aucune — les mêmes conditions d'affichage que celles du module source s'appliquent (voir le
`docs/rbac/<module-source>.md` de chaque type d'opération).

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Valider une opération de n'importe quel module source (unitaire ou par lot) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Annuler une transaction | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — dérogation assumée à `security.md`, bloquée si le rapprochement est `CLOTURE_DEFINITIVE` (voir `docs/rbac/transactions.md`) |

*Le module États des encours agrège en lecture seule l'ensemble des modules précédents.*

<a id="chap29"></a>
## Chapitre 29 — États des encours et Récapitulatif

Rôle utilisé dans ce document :

```java
static final String FINANCE_OR_ADMIN =
        "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
```

---

### Endpoints — `RecapitulatifController` (base `/recapitulatif`)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 5 endpoints d'un coup.

| Méthode | URL | Description |
|---|---|---|
| GET | `/recapitulatif/banque/{banqueId}/par-source` | Indicateurs par source (nombre d'opérations, total débit, total crédit, nombre d'échues), mêmes critères que l'endpoint ci-dessous, sans pagination — alimente les cartes de la vue d'ensemble et la liste des sources de la vue focus de l'écran Récapitulatifs. |
| GET | `/recapitulatif/banque/{banqueId}` | Encaissements **et** décaissements prévisionnels jusqu'à l'horizon, entrelacés, paginés/triés/filtrés — alimente l'écran unique Récapitulatifs. Le sens est un critère optionnel (`?sens=ENCAISSEMENT` \| `DECAISSEMENT`) : absent, les deux sens sont retournés. L'horizon est un critère optionnel (`?periode=FIN_DE_MOIS` par défaut \| `FIN_DE_TRIMESTRE`, 14/10/2026). |
| GET | `/recapitulatif/toutes-banques` | Nouveau (17/10/2026) — encours à valider de **toutes** les banques jusqu'à l'horizon (`?periode=FIN_DE_MOIS` par défaut \| `FIN_DE_TRIMESTRE`), une ligne par encours avec sa banque (`banqueId`) et sa date de référence, en un seul appel. Lecture seule ; même `@PreAuthorize` de classe que les autres endpoints du contrôleur (`FINANCE_OR_ADMIN`) : aucun changement de droits. Alimente les filtres d'opération et les jalons de la page globale de la Trésorerie prévisionnelle. |
| GET | `/recapitulatif/banque/{banqueId}/export/pdf` | Export PDF du récapitulatif, mêmes critères que l'endpoint ci-dessus (filtre de sens compris), sans pagination. |
| GET | `/recapitulatif/{banqueId}/pdf` | Récapitulatif global (PDF) d'une banque. **Aucun déclencheur dans l'UI actuellement** — `RecapService.downloadRecapGlobalByBank()` existe côté frontend mais n'est appelé par aucun composant ; endpoint disponible via Swagger uniquement. |
| GET | `/recapitulatif/global/pdf` | Récapitulatif global consolidé (PDF, toutes banques) — bouton "Récap global" du tableau de bord Finance. `?periode=A_CE_JOUR` \| `FIN_DE_MOIS` (défaut) \| `FIN_DE_TRIMESTRE`. |

> **Rupture de contrat (06/09/2026).** Les quatre endpoints
> `/recapitulatif/{decaissement,encaissement}/banque/{banqueId}` et leurs variantes `/export/pdf`
> n'existent plus : les deux écrans Récapitulatif Décaissements et Récapitulatif Encaissements ont
> été fusionnés en un seul, et le sens est passé de segment d'URL à critère de recherche optionnel.
> Le périmètre d'autorisation est inchangé — même `@PreAuthorize` au niveau classe, mêmes rôles.

---

### Endpoints — `RecapGeneralDetailleController` (base `/recap-general-detaille`)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 4 endpoints d'un coup.

| Méthode | URL | Description |
|---|---|---|
| GET | `/recap-general-detaille/banque/{banqueId}` | Récapitulatif général détaillé (JSON) d'une banque. **Aucun déclencheur dans l'UI** — seule la version PDF est consommée par le frontend. |
| GET | `/recap-general-detaille/all` | Récapitulatif général détaillé (JSON), toutes banques. **Aucun déclencheur dans l'UI** — idem. |
| GET | `/recap-general-detaille/banque/{banqueId}/pdf` | Récapitulatif général détaillé (PDF) d'une banque — bouton "Récap détaillé" sur la carte de la banque, tableau de bord Finance. |
| GET | `/recap-general-detaille/all/pdf` | Récapitulatif général détaillé (PDF), toutes banques — bouton "Récap détaillé" d'en-tête, tableau de bord Finance. |

Les quatre endpoints acceptent `?periode=FIN_DE_MOIS` (défaut) \| `FIN_DE_TRIMESTRE` (14/10/2026) : simple
paramètre de lecture, même `@PreAuthorize`.

---

### Endpoints — `TresoreriePrevisionnelleController` (base `/tresorerie-previsionnelle`, 14/10/2026)

`@PreAuthorize(FINANCE_OR_ADMIN)` posé au niveau classe — couvre les 2 endpoints. Lecture seule : aucun
endpoint d'écriture.

| Méthode | URL | Description |
|---|---|---|
| GET | `/tresorerie-previsionnelle` | Plan de trésorerie prévisionnel (JSON) : détail par banque + un total par devise, `?periode=FIN_DE_MOIS` (défaut) \| `FIN_DE_TRIMESTRE` — écran Trésorerie prévisionnelle et page d'une banque (chaque ligne de catégorie liste ses sources depuis le 17/10/2026). |
| GET | `/tresorerie-previsionnelle/pdf` | Export PDF du plan, `?periode=…`, puis `?banqueId=` (une banque ; inconnue = 404) ou `?devise=` (le total d'une devise ; inconnue = 404 ; absent = dirham) — boutons « Exporter PDF » de l'écran. |

Couverture de test : `TresoreriePrevisionnelleIntegrationTest` (administrateur → 200 sur les deux endpoints ;
utilisateur hors Finance → 403 sur les deux ; banque inconnue → 404).

---

### Guards Angular

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/etats-des-encours/:banqueId` (parent — s'applique à `select` et à `:rapprochementId` avec tous ses enfants) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours (accès transversal). |
| `/portail/etats-des-encours/:banqueId/:rapprochementId` | `rapprochementActifGuard` | Ne vérifie aucun rôle — s'assure que `rapprochementId` est un identifiant numérique valide dans l'URL ; sinon redirige vers l'écran de sélection (`/select`), ou vers l'accueil si `banqueId` est lui-même introuvable. Garde de navigation, pas de sécurité d'accès. |
| `/portail/tresorerie-previsionnelle` (14/10/2026) | `departmentGuard('Finance')` | `DEPT_Finance:MEMBER` ou `DEPT_Finance:DIRECTOR`. `SYSTEM_ADMIN` passe toujours. Les paramètres d'adresse `?horizon=` et `?devise=` ne sont que des critères de lecture (valeur inconnue → valeur par défaut) ; l'ancien `?banque=…&col=…` redirige vers la page de la banque. |
| `/portail/tresorerie-previsionnelle/banque/:banqueId` (17/10/2026) | `departmentGuard('Finance')` (hérité de la route parente) | Même règle. Page du plan d'une banque : appelle `GET /tresorerie-previsionnelle`, `GET /recapitulatif/banque/{banqueId}` et `GET /tresorerie-previsionnelle/pdf?banqueId=` (mêmes `@PreAuthorize`). Les paramètres `?horizon=`, `?vue=`, `?sens=`, `?sources=`, `?op=`, `?montant=`, `?q=`, `?ouvert=`, `?col=` ne sont que des critères de lecture (valeur inconnue → valeur par défaut). Aucune autorisation nouvelle. |

---

### Visibilité sidebar

L'espace de travail États des encours n'a pas d'entrée dans la barre latérale principale : son point
d'entrée est la carte d'une banque sur le tableau de bord Finance (route directe vers l'écran de
sélection du rapprochement actif).

| Item | `requiredAuthority` | Route |
|---|---|---|
| Trésorerie prévisionnelle (14/10/2026) | `['DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR']` (`SYSTEM_ADMIN` voit toujours tout) | `/portail/tresorerie-previsionnelle` |

Le tableau de bord Finance mène aussi à cet écran (icône de courbe de chaque carte ; il n'y a plus de
bouton « Trésorerie » dans l'en-tête, l'accès général passe par le menu latéral) : mêmes conditions, puisque le tableau de bord est lui-même réservé à Finance.

---

### Actions conditionnées dans l'UI

Consultation, validation individuelle ou en lot, clôture, téléchargement PDF : disponibles à tout
utilisateur ayant passé le `departmentGuard('Finance')`, sans condition de rôle supplémentaire.

**Horizon fin de mois / fin de trimestre (14/10/2026) — aucune autorisation modifiée.** La liste déroulante
d'horizon (tableau de bord, Récapitulatifs, Trésorerie prévisionnelle) ne fait qu'ajouter un paramètre de
lecture `periode` aux endpoints existants, et `?horizon=` à l'adresse des écrans ; il est proposé à tout
utilisateur qui a accès à l'écran, sans condition de rôle.

**Popup des opérations en cours d'une banque (17/10/2026) — aucune autorisation modifiée.** Les lignes
« Encours entrées / sorties » des cartes du tableau de bord ouvrent un popup qui appelle l'endpoint existant
`GET /recapitulatif/banque/{banqueId}` (même `@PreAuthorize`, paramètres `periode` et `sens` de lecture ; chaque
ligne porte désormais aussi sa `dateReference`, déjà utilisée par le filtre d'horizon). Aucun nouvel endpoint, aucun guard ni élément de navigation ajouté ; le popup est visible de tout utilisateur qui voit le
tableau de bord. L'horizon repart de la fin de mois après un rechargement de page : simple état d'interface, sans
effet sur les droits.

**Vue focus, filtres par source, état dans l'adresse (12/10/2026) — aucune autorisation modifiée.**
L'écran Récapitulatifs garde son état (source ouverte, filtres globaux et de la source, page, tri, ligne à
surligner) dans les paramètres d'adresse (`?source=…&page=…`) : ce sont de simples critères de lecture,
relus à chaque chargement et jamais interprétés comme des droits ; une valeur inconnue retombe sur sa valeur
neutre, et le tri n'accepte qu'une liste blanche de colonnes. Les endpoints appelés sont les mêmes
(`GET /recapitulatif/banque/{banqueId}`, `…/par-source`, `…/export/pdf`) avec le même `@PreAuthorize`. Le bouton
« Ajouter » transmet désormais `?banqueId=` à l'écran de création (présélection de la banque) : les écrans
et endpoints de création restent protégés par leurs propres gardes, inchangés. Le bouton « Ajouter » existe
à deux niveaux — barre du haut (fenêtre « Ajouter une opération », composant `AjouterOperationDialogComponent`,
toutes les sources sauf leasing, CMT, mises à disposition et cautions ; une source sans route de création est
grisée) et en-tête de chaque source (affiché seulement si une route de création est déclarée,
`RecapitulatifComponent.CREATE_ROUTE`) ; aucune règle d'autorisation propre : seule la route cible compte. Le lien « échues · voir » d'une carte
n'ajoute qu'un critère de lecture (borne haute de la date de référence).

**Ajouter / Éditer (20/09/2026) — conditionnées par (source, sens), pas par un rôle.**
`RecapitulatifComponent` route désormais vers l'écran dédié de création/édition de chaque source
(mêmes pages, mêmes gardes `departmentGuard('Finance')` et mêmes endpoints `@PreAuthorize` que
documentés dans le chapitre RBAC de chaque source — aucun nouvel endpoint, aucune autorisation
modifiée) plutôt que de reconstruire un formulaire dans le récap. Le bouton « Éditer » par ligne et
le bouton « Ajouter » de l'en-tête de chaque source ne s'affiche que pour les combinaisons `(source, sens)` listées
ci-dessous (`RecapitulatifComponent.CREATE_EDIT_ELIGIBILITY`/`CREATE_ROUTE`/`EDIT_ROUTE`) — matrice
figée le 20/09/2026, reprise à l'identique du comportement des anciens écrans séparés par source :

| Source | Sens éligible | Route Ajouter | Route Éditer |
|---|---|---|---|
| Virement | Encaissement uniquement | `/portail/Virements/new` | `/portail/Virements/edit/:id` |
| Chèque | Encaissement uniquement | `/portail/Cheques/new` | `/portail/Cheques/edit/:id` |
| LCN Encaissement | Les deux sens (nature imposée par le formulaire, pas par le récap) | `/portail/LCN/new` | `/portail/LCN/edit/:id` |
| LCN Escompte (21/09/2026) | Les deux sens — même formulaire/id-space que LCN Encaissement (`LcnFormComponent`, contrôle `natureLcn`) | — (`CREATE_ROUTE` n'a pas d'entrée dédiée pour LCN Escompte : même route que LCN Encaissement, `/portail/LCN/new`) | `/portail/LCN/edit/:id` |
| Virement international | Les deux sens | `/portail/virements-internationaux/new` | `/portail/virements-internationaux/edit/:id` |
| Préfinancement | Les deux sens | `/portail/préfinancements/new` | `/portail/préfinancements/edit/:id` |
| Droit constaté | Les deux sens | `/portail/droits-constate/new` | `/portail/droits-constate/edit/:id` |
| Factoring | Les deux sens | `/portail/factoring/new` | `/portail/factoring/edit/:id` |
| Décomptes | Les deux sens (encaissement seul atteignable en pratique) | `/portail/decomptes/new` | `/portail/decomptes/edit/:id` |
| Restitution | Les deux sens (encaissement seul atteignable en pratique) | `/portail/restitutions/new` | `/portail/restitutions/edit/:id` |
| Impayée | Les deux sens | `/portail/impayees/new` | `/portail/impayees/edit/:id` |
| Provision | Les deux sens | `/portail/provisions/new` | `/portail/provisions/edit/:id` |
| Commission | Les deux sens (décaissement seul atteignable en pratique) | `/portail/commissions/new` | `/portail/commissions/edit/:id` |
| Prélèvement | Les deux sens (décaissement seul atteignable en pratique) | `/portail/prelevements/new` | `/portail/prelevements/edit/:id` |
| Frais bancaire | Les deux sens (décaissement seul atteignable en pratique) | `/portail/frais-bancaire/new` | `/portail/frais-bancaire/edit/:id` |
| Opération erronée | Les deux sens (décaissement seul atteignable en pratique) | `/portail/operations/errones/new` | `/portail/operations/errones/edit/:id` |
| Impôts et taxes | Les deux sens (décaissement seul atteignable en pratique) | `/portail/impots-taxes/new` | `/portail/impots-taxes/edit/:id` |
| Leasing (21/09/2026) | Éditer uniquement (échéance `PREVU`), réservé `SYSTEM_ADMIN` | — (aucun endpoint de création d'une échéance seule n'existe) | `/portail/leasing/echeances/edit/:id` |
| CMT (21/09/2026) | Éditer uniquement (échéance `PREVU`), réservé `SYSTEM_ADMIN` | — (aucun endpoint de création d'une échéance seule n'existe) | `/portail/cmt/echeances/edit/:id` |

**Leasing/CMT — Éditer ajouté le 21/09/2026, dialogue remplacé par une page routée.** Le bouton
« Éditer » par ligne réutilise `authService.isSystemAdmin()` en plus de `CREATE_EDIT_ELIGIBILITY`
(voir `EDIT_REQUIRES_SYSTEM_ADMIN` dans `recapitulatif.component.ts`) pour ne s'afficher qu'aux
administrateurs système, comme l'ancien dialogue sur l'écran de tableau d'amortissement. La
restriction « échéance déjà payée » (statut `PAYE`) n'est pas vérifiable par le récap lui-même
(`RecaputilatifItem` ne porte pas ce champ) : elle est vérifiée à l'ouverture de la page d'édition
(après un `GET /echeances-leasing|cmt/{id}`) et, de toute façon, déjà bloquée par le backend
(`PUT .../{id}` renvoie 400 si le statut n'est pas `PREVU`). Aucun bouton « Ajouter » pour ces deux
sources : aucun endpoint de création d'une échéance seule n'existe (l'échéancier est généré à la
création du contrat entier), donc `CREATE_ROUTE` n'a volontairement pas d'entrée pour elles.

**Aucun bouton Ajouter/Éditer** pour Mise à disposition, Agios, `LCN_DECAISSEMENT`, Virement/Chèque
en décaissement — comme dans les anciens écrans dédiés à ces combinaisons. Le retour au récap après
un Ajouter/Éditer réussi repose sur `Location.back()`, déjà appelé par chaque formulaire en cas de
succès — aucun mécanisme de retour n'a été ajouté spécifiquement pour le récap.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Accéder à l'espace de travail États des encours (Angular) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Consulter le Récapitulatif, tous sens confondus ou filtré (API) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Exporter en PDF le Rapprochement en cours, le Récapitulatif ou l'encours d'un type d'opération | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — même niveau que la consultation correspondante |
| Consulter ou télécharger le récapitulatif général détaillé (API) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Consulter la Trésorerie prévisionnelle et exporter son plan (Angular + API) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
| Valider un encours (individuel ou en lot) | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` — voir Chapitre 28 — Validation |

*Les modules suivants sont transverses.*

<a id="chap30"></a>
## Chapitre 30 — Logs

---

### Endpoints (`LogController`, base `/logs`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| GET | `/logs` | `SYSTEM_ADMIN` | Le journal d'audit expose des informations sensibles (utilisateur, entité, valeurs avant/après) sur l'ensemble des modules — réservé à l'admin système, cohérent avec le guard frontend déjà en place. |
| DELETE | `/logs/{id}` | `SYSTEM_ADMIN` | Règle transverse (`conventions/security.md`, "Suppression") : suppression toujours réservée à `SYSTEM_ADMIN`. |
| GET | `/logs/{id}/details` | `SYSTEM_ADMIN` | Idem consultation — détail des valeurs avant/après. |
| GET | `/logs/entities` | `SYSTEM_ADMIN` | Idem consultation — liste des entités auditées, utilisée pour peupler le filtre. |

Le détail champ-par-champ est chargé par le frontend à l'ouverture de la fenêtre de détail
(`GET /logs/{id}/details`) et ne transite jamais par la réponse de liste — la restriction
`SYSTEM_ADMIN` porte donc bien sur les deux endpoints, sans divergence entre backend et frontend.

**Avant le sweep de conformité, aucun `@PreAuthorize` n'existait sur ce contrôleur** — tout
utilisateur authentifié, quel que soit son département, pouvait consulter et supprimer des entrées de
journal via l'API. Seul le frontend restreignait l'accès (`systemAdminGuard`). Corrigé : les 4
endpoints sont désormais réservés `SYSTEM_ADMIN` côté backend, alignés sur la restriction frontend
déjà en place.

---

### Guards Angular (`portail.routes.ts`)

| Route | Guard | Rôle vérifié |
|---|---|---|
| `/portail/logs` | `systemAdminGuard` | `SYSTEM_ADMIN` uniquement. |

Guard déjà en place avant ce sweep — aucun changement nécessaire.

---

### Visibilité sidebar (`sidebar.component.ts`)

| Item | `requiredAuthority` |
|---|---|
| "Les Journaux du portail" (sous le groupe "Paramètres") | Aucun `requiredAuthority` propre — hérite de la restriction `SYSTEM_ADMIN` du groupe parent "Paramètres". |

Déjà en place avant ce sweep — aucun changement nécessaire.

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Bouton "Supprimer" (une entrée de journal) | Accessible dès lors que la page est atteinte (protégée en amont par `systemAdminGuard`) | `LogsComponent` |

Aucune divergence backend/frontend : l'accès à la page entière est déjà réservé `SYSTEM_ADMIN` côté
frontend, désormais aligné par un `@PreAuthorize(SYSTEM_ADMIN)` sur chaque endpoint côté backend.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / supprimer une entrée de journal (API + écran) | `SYSTEM_ADMIN` uniquement |

<a id="chap31"></a>
## Chapitre 31 — Notifications

---

### Endpoints (`NotificationController`, base `/notification`)

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/notification/public` | `SYSTEM_ADMIN` | Déclenchement manuel d'une notification publique — action d'administration, pas une donnée personnelle. |
| GET | `/notification/history` | `isAuthenticated()` | Consultation de ses propres notifications (tous statuts) — donnée personnelle, ouverte à tout utilisateur connecté. |
| GET | `/notification/unread` | `isAuthenticated()` | Idem, notifications non lues. |
| GET | `/notification/read` | `isAuthenticated()` | Idem, notifications lues. |
| PUT | `/notification/{id}/mark-as-read` | `isAuthenticated()` | Marquer sa propre notification comme lue — donnée personnelle. |

**Avant le sweep de conformité, aucun `@PreAuthorize` n'existait sur ce contrôleur** — tout
utilisateur authentifié pouvait notamment déclencher une notification publique globale
(`POST /notification/public`), une action qui devrait être réservée à l'administration. Corrigé :
seul cet endpoint est désormais réservé `SYSTEM_ADMIN`, les 4 autres (consultation/marquage de ses
propres notifications) restent ouverts à tout utilisateur authentifié — décision explicite de HMAYDA
(pas de restriction Finance/département, ces notifications concernent tous les profils).

Les 4 writers batch (Leasing, Crédit Moyen Terme, Lignes Fonctionnelles générale et spécifique)
appellent directement `INotificationService.sendPublicNotification(...)` côté serveur — hors
périmètre HTTP, aucun `@PreAuthorize` applicable.

---

### Guards Angular

Aucune route Angular dédiée : les notifications sont consultées via un composant (liste) affiché
dans une fenêtre modale ouverte depuis la barre de navigation, pas via une page routée. L'accès
hérite du guard d'authentification générique posé sur `/portail` (`authGuard`) — aucune restriction
de rôle supplémentaire, cohérent avec le RBAC backend (`isAuthenticated()` sur les 4 endpoints de
consultation/marquage).

---

### Visibilité sidebar / navbar

Aucune entrée sidebar. Dans la navbar (`navbar.component.ts`), l'icône de notifications n'est pas
soumise à `requiredAuthority` — visible pour tout utilisateur connecté, cohérent avec le RBAC
backend.

---

### Actions conditionnées dans l'UI

| Action | Condition d'affichage | Composant |
|---|---|---|
| Icône notifications + compteur non lues | Visible pour tout utilisateur connecté | `NavbarComponent` |
| Marquer comme lue | Visible pour tout utilisateur connecté | `NotificationListComponent` |

Aucun écran ne permet de déclencher une notification publique (`POST /notification/public`) — cette
action n'est exposée qu'au niveau API à ce jour.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Consulter / marquer comme lues ses propres notifications (API + écran) | Tout utilisateur authentifié |
| Déclencher une notification publique (API uniquement) | `SYSTEM_ADMIN` uniquement |

<a id="chap32"></a>
## Chapitre 32 — Synchronisation FunPaiement

---

### Endpoint (`EtatsDesEncoursController`, base `/funPayment`)

**Avant le sweep de conformité (11/07/2026), cette classe n'avait aucun `@PreAuthorize` : le
déclenchement manuel de la synchronisation était accessible à tout utilisateur authentifié, quel
que soit son département.** Corrigé.

| Méthode | URL | `@PreAuthorize` | Justification |
|---|---|---|---|
| POST | `/funPayment/synchronisation` | `FINANCE_OR_ADMIN` (`SYSTEM_ADMIN`, `DEPT_Finance:MEMBER`, `DEPT_Finance:DIRECTOR`) | Déclenchement manuel réservé à l'équipe Finance ou à l'admin système — aligné sur la restriction déjà appliquée côté frontend (voir ci-dessous). |

Il n'existe pas d'endpoint de suppression pour ce module (rien à restreindre à `SYSTEM_ADMIN`).

La synchronisation planifiée automatique (`PaymentBatchScheduler`, cron lundi-vendredi 13h/20h,
Africa/Casablanca) ne passe pas par ce contrôleur et n'est donc concernée par aucune règle RBAC —
elle s'exécute en tâche de fond, hors contexte utilisateur.

---

### Frontend

Le bouton "Synchroniser FunPaiement" est exposé depuis le tableau de bord Finance
(`acceuil.component.ts`), sur la route `/accueil/finance`, déjà protégée par
`departmentGuard('Finance')` (`portail.routes.ts`). Avant la correction ci-dessus, cette protection
frontend n'avait pas d'équivalent côté API — un utilisateur pouvait contourner le guard Angular en
appelant directement `POST /funPayment/synchronisation`. Les deux couches sont désormais alignées.

---

### Résumé

| Action | Qui peut faire |
|---|---|
| Déclencher manuellement la synchronisation FunPaiement | Équipe Finance (membre ou directeur), `SYSTEM_ADMIN` |
