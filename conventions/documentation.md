# Documentation

> Toute fonctionnalité du projet doit être documentée. La documentation fait partie du code et évolue avec lui.

# Principes

* Aucune fonctionnalité ne doit rester non documentée.
* Toute modification du code implique une mise à jour de la documentation.
* Une tâche n'est terminée que lorsque le code **et** la documentation sont synchronisés.
* La documentation est la source de vérité fonctionnelle et technique du projet.

---

# Arborescence

```text
docs/
├── architecture/
├── modules/
├── rbac/
├── direction/
└── guide-utilisateur/
```

---

# Mise à jour obligatoire

Après chaque création, modification ou suppression d'une fonctionnalité, mettre à jour les documents concernés avant de passer à une autre tâche.

Ne jamais repousser cette mise à jour.

---

# Documentation des modules

Chaque domaine métier possède une fiche :

```text
docs/modules/<module>.md
```

Cette fiche s'adresse à un public métier (équipe Finance, chef de projet, futur développeur métier) et doit permettre de comprendre entièrement le fonctionnement du module **sans lire le code et sans aucun détail technique** (voir `CLAUDE.md` racine, section "Documentation").

Elle ne contient donc jamais :

* noms de classes, méthodes, interfaces, annotations Java ou TypeScript ;
* noms de tables, colonnes, index ou contraintes SQL ;
* noms de composants Angular, routes, services, DTOs ;
* détails d'implémentation (Spring, JPA, Hibernate, MapStruct, etc.) ;
* structure de packages ou arborescence de fichiers.

Le détail technique correspondant a sa place ailleurs :

* structure du code, composants, interactions internes → `docs/architecture/` ;
* endpoints, `@PreAuthorize`, guards Angular, sidebar → `docs/rbac/<module>.md` ;
* contrat API (requêtes/réponses/codes HTTP/erreurs) → Swagger/OpenAPI (`conventions/swagger.md`), déjà maintenu dans le code ;
* écrans, navigation, actions utilisateur → `docs/guide-utilisateur/`.

Elle documente obligatoirement, au niveau fonctionnel uniquement :

## Présentation

* objectif métier ;
* rôle du module ;
* dépendances avec les autres modules.

---

## Entités métier

Pour chaque notion/entité métier du module (pas l'entité JPA) :

* rôle ;
* description en langage métier ;
* règles particulières.

---

## Sécurité

Documenter, au niveau fonctionnel uniquement (qui peut faire quoi) :

* rôles autorisés ;
* permissions.

Le détail technique (`@PreAuthorize`, guards Angular, visibilité dans la sidebar) n'a pas sa place ici — voir `docs/rbac/<module>.md` (section "Documentation RBAC").

---

## Audit

Documenter, en langage métier (jamais Envers, `@Audited`, `@AuditableAction`, etc.) :

* quelles actions sur quelles notions métier sont historisées (ex. "toute création/modification/suppression d'un compte est tracée avec auteur et date") ;
* ce qui est volontairement exclu de cet historique ;
* la disponibilité de cet historique pour consultation.

---

## Logs

Documenter, en langage métier (jamais de niveau de log, de nom de méthode ou de message de log) :

* quels traitements produisent un événement journalisé utile au diagnostic ;
* quelles anomalies gérées (blocages, tentatives invalides) sont également journalisées.

---

## Règles métier

Documenter **toutes** les règles métier.

Exemples :

* validations ;
* calculs ;
* transitions d'état ;
* blocages ;
* exceptions métier ;
* cas particuliers ;
* effets de bord.

Aucune règle métier ne doit exister uniquement dans le code.

---

## Workflow

Décrire le cycle de vie complet de la fonctionnalité.

Exemple :

```text
Création
    ↓
Validation
    ↓
Traitement
    ↓
Clôture
    ↓
Archivage
```

---

## Diagrammes

Ajouter lorsque pertinent :

* diagramme de séquence ;
* diagramme de classes ;
* diagramme d'état ;
* diagramme de flux.

---

# Documentation Direction

Chaque module possède également :

```text
docs/direction/<module>.md
```

Cette documentation est destinée aux responsables métier.

Elle décrit :

* le besoin métier ;
* les utilisateurs concernés ;
* les fonctionnalités ;
* les bénéfices ;
* les principales règles ;
* les cas d'utilisation ;

Aucun détail technique ne doit y apparaître.

---

# Documentation RBAC

Chaque module possède également :

```text
docs/rbac/<module>.md
```

Cette documentation est destinée aux développeurs et aux revues de sécurité. Elle permet de savoir qui peut faire quoi sur un module, sans avoir à relire le code.

Elle décrit :

* chaque endpoint (méthode HTTP + URL) et l'expression `@PreAuthorize` qui le protège ;
* chaque guard Angular posé sur les routes du module ;
* la visibilité des éléments de sidebar/navigation (`requiredAuthority`) ;
* les conditions d'affichage des actions sensibles dans l'UI ;
* toute divergence assumée entre restriction backend et restriction frontend, avec sa justification.

Aucune logique métier, aucune justification métier et aucune instruction utilisateur ne doit y apparaître — voir respectivement `docs/modules/`, `docs/direction/` et `docs/guide-utilisateur/`.

Toute évolution des autorisations d'un module (nouveau rôle, nouvel endpoint, changement de `@PreAuthorize` ou de guard) implique la mise à jour de sa fiche RBAC dans le même changement.

---

# Guide utilisateur

Chaque profil possède son guide.

```text
docs/guide-utilisateur/
├── admin.md
├── finance.md
├── ...
```

Chaque guide documente :

* les fonctionnalités accessibles ;
* la navigation ;
* les écrans ;
* les actions possibles ;
* les procédures ;
* les messages d'erreur ;
* les bonnes pratiques.

---

# Documentation d'architecture

Le dossier :

```text
docs/architecture/
```

décrit :

* l'architecture globale ;
* les domaines métier ;
* les dépendances entre modules ;
* les flux applicatifs ;
* les décisions d'architecture ;
* les composants transverses (authentification, audit, logging, sécurité, métriques...).

---

# Critère de complétude

Une documentation est complète lorsqu'un développeur, un testeur ou un utilisateur métier peut comprendre, utiliser et faire évoluer une fonctionnalité sans avoir à lire le code source.

Aucune information ne doit exister uniquement dans le code.
