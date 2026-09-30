# G-Banque

> G-Banque est le système de gestion et de suivi des opérations bancaires internes de **Staport**,
> une entreprise du BTP (bâtiment et travaux publics).
>
> Ce document est la porte d'entrée du projet.
>
> Les conventions détaillées sont définies dans `conventions`.
> Elles constituent la source de vérité et doivent être respectées lors de toute modification.

---

# Principes

Avant toute modification :

- comprendre le besoin ;
- identifier le ou les modules concernés ;
- respecter les conventions existantes ;
- privilégier la cohérence de l'architecture.

Après toute modification :

- vérifier que le projet compile ;
- supprimer le dead code ;
- mettre à jour la documentation concernée.

Une tâche n'est terminée que lorsque le code, la documentation et les conventions sont synchronisés.

---

# Ordre de lecture

1. conventions/architecture.md
2. conventions/design-patterns.md
3. conventions/configuration.md
4. conventions/jpa.md
5. conventions/database.md
6. conventions/security.md
7. conventions/audit.md
8. conventions/logging.md
9. conventions/swagger.md
10. conventions/metrics.md
11. conventions/performance.md
12. conventions/frontend.md
13. conventions/documentation.md

En cas de conflit entre plusieurs conventions :

- `architecture.md` fait foi pour toutes les décisions d'architecture ;
- les autres documents complètent cette architecture dans leur domaine.

---

# Documentation

Toute évolution d'une fonctionnalité implique la mise à jour de la documentation correspondante.

La documentation est organisée en quatre types distincts. Chaque type a une audience et un contenu précis. **Ne jamais mélanger les niveaux.**

Pour `docs/modules/`, `docs/direction/` et `docs/rbac/`, chaque type tient dans **un seul fichier `README.md`** (et non un fichier par module) : le document est structuré en chapitres, un chapitre par module, dans un ordre métier fixe et partagé par les trois types (socle d'accès → comptes bancaires → produits financiers rattachés au compte → marchés et satellites → moyens de paiement → transactions → charges appliquées → anomalies → rapprochement → états des encours → transverse). Une modification d'un module implique de mettre à jour le chapitre correspondant dans les trois `README.md`, pas de créer un nouveau fichier. `docs/guide-utilisateur/` reste organisé par rôle (`admin.md`, `finance.md`), pas par module.

---

## `docs/modules/` — Spécification fonctionnelle

**Audience :** équipe Finance, chef de projet, futur développeur métier — des profils non
techniques.
**Objectif :** comprendre exhaustivement le fonctionnement métier du module sans lire le code.

Ce document doit couvrir **tout** ce qui relève du fonctionnel : chaque règle métier, chaque cas
particulier, chaque valeur par défaut, chaque comportement attendu et chaque dépendance avec les
autres modules. Rien ne doit être laissé de côté, supposé évident, ou renvoyé implicitement au code.

À l'inverse, ce document ne doit contenir **aucun détail technique** : le lecteur n'est pas
développeur et ne doit jamais avoir besoin d'ouvrir le code pour comprendre le fonctionnement
métier.

---

## `docs/direction/` — Note de synthèse pour la direction

**Audience :** direction, management, décideurs.
**Objectif :** valoriser le travail réalisé sur G-Banque, mettre en avant ce que le module apporte
concrètement au quotidien de Staport, et démontrer le retour sur investissement.

Ce document doit parler du **projet lui-même** : ce qui a été construit, ce que ça permet de faire
aujourd'hui, et les bénéfices concrets pour l'entreprise (gain de temps, fiabilité, maîtrise des
risques financiers, visibilité pour la direction, etc.).

Il ne doit **jamais** comparer à une situation "avant G-Banque" ou à un ancien fonctionnement — cette
situation antérieure n'est pas documentée et ne doit jamais être supposée ou inventée. Le document se
concentre uniquement sur ce que G-Banque apporte, présenté comme un acquis et un atout du projet.

---

## `docs/guide-utilisateur/` — Guide d'utilisation

**Audience :** utilisateurs finaux (équipe Finance, administrateurs) — des profils non techniques.
**Objectif :** guider pas à pas, de façon exhaustive, dans l'utilisation de l'application.

Ce document doit couvrir **tout** ce que l'utilisateur peut faire dans l'application : chaque
écran, chaque champ, chaque action possible (et comment la déclencher), chaque comportement
attendu après une action, chaque message d'erreur courant, et les droits requis pour chaque action.
Rien ne doit être laissé de côté ou supposé déjà connu de l'utilisateur.

À l'inverse, ce document ne doit contenir **aucun détail technique** : le lecteur n'est pas
développeur.

---

## `docs/rbac/` — Suivi des accès par module

**Audience :** développeurs, revue de sécurité, audit.
**Objectif :** savoir, pour un module donné, qui peut faire quoi — de façon exhaustive et vérifiable, sans avoir à relire le code.

Chaque module possède son chapitre dans le fichier unique :

```text
docs/rbac/README.md
```

Ce que ce document DOIT contenir :
- chaque endpoint du module (méthode HTTP + URL) et l'expression `@PreAuthorize` qui le protège ;
- chaque guard Angular posé sur les routes du module (`systemAdminGuard`, `departmentGuard(...)`, etc.) ;
- la visibilité des éléments de sidebar/navigation et l'autorité requise (`requiredAuthority`) ;
- les conditions d'affichage des actions sensibles dans l'UI (ex. bouton de suppression réservé à `isSystemAdmin()`) ;
- toute divergence assumée entre restriction backend et restriction frontend, avec sa justification.

Ce que ce document NE DOIT PAS contenir :
- la logique métier ou les règles de calcul (renvoyer à `docs/modules/`)
- la justification métier du besoin (renvoyer à `docs/direction/`)
- des instructions pas-à-pas destinées à l'utilisateur final (renvoyer à `docs/guide-utilisateur/`)

Toute création de module, ou toute modification des autorisations d'un module existant (nouveau rôle, nouvel endpoint, changement de `@PreAuthorize` ou de guard), implique la mise à jour du chapitre correspondant dans `docs/rbac/README.md` dans le même changement.

---

La documentation fait partie du code. Une fonctionnalité non documentée dans les quatre niveaux n'est pas terminée.
---

# Décisions d'architecture

Les conventions définissent les principes généraux du projet, mais elles ne remplacent pas le jugement d'architecture.

Lorsqu'une évolution nécessite un choix d'architecture important (nouveau Design Pattern, refactoring majeur, changement de structure, introduction d'une nouvelle abstraction ou modification d'un composant transversal), ne jamais prendre cette décision seul.

Dans ce cas :

- expliquer brièvement le problème identifié ;
- proposer les différentes approches possibles ;
- recommander celle qui respecte le mieux les conventions du projet ;
- attendre la validation avant de modifier l'architecture.

Ne jamais introduire un Design Pattern, une abstraction ou une refonte importante sans nécessité clairement identifiée.

---
# En cas de doute

Ne jamais faire d'hypothèse sur :

- une règle métier ;
- une décision d'architecture ;
- une convention ;
- une valeur métier ;
- le comportement attendu.

Si une information est manquante ou ambiguë :

- poser une question avant de modifier le code ;
- ne jamais inventer un comportement ;
- ne jamais ajouter une fonctionnalité ou une abstraction non demandée.

Si plusieurs approches sont possibles :

- expliquer brièvement les différentes options ;
- proposer celle qui respecte le mieux les conventions du projet ;
- attendre validation avant de réaliser une modification importante.

Privilégier une question plutôt qu'une mauvaise hypothèse.

---

# Règles générales

Ne jamais :

- contourner une convention existante ;
- introduire de la dette technique ;
- dupliquer une logique métier ;
- laisser du dead code ;
- laisser une fonctionnalité ou une règle métier non documentée.

Toujours privilégier :

- un code simple ;
- une architecture cohérente ;
- une documentation complète ;
- la maintenabilité à long terme.

En cas de doute, les documents présents dans `docs/conventions/` sont la source de vérité du projet.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Imported Claude Cowork project instructions
