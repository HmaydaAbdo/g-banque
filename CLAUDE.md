# G-Banque — Conventions Backend

> Lu automatiquement par Claude au démarrage de chaque session — fait foi sur les conventions à respecter.

## Architecture par module

- Entity (JPA) — jamais exposée directement en réponse API.
- Repository : `JpaRepository` (+ `JpaSpecificationExecutor` si filtre dynamique). `@EntityGraph(attributePaths = {...})` pour toute relation chargée en sérialisation JSON — jamais `spring.jpa.open-in-view`.
- Specification statique `XSpecification.searchByCriteria(XSearchCriteria)` pour toute liste filtrable/paginée.
- Mapper MapStruct (`@Mapper(componentModel = "spring")`) obligatoire pour tout nouveau module. Modules historiques (Bank, Caution) gardent leur mapper manuel — pas de migration rétroactive.
- DTO : records Java, validation Jakarta complète (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`...). Request/Response séparés, jamais l'entité en retour d'API.
- Service : `@Transactional` sur toute méthode mutante. Lookup d'entité dans une méthode privée non exposée hors du service.
- Controller "thin" : DTOs uniquement, mapping délégué au service. `@PreAuthorize` sur chaque endpoint ou au niveau classe.
- `@RestControllerAdvice(assignableTypes = XController.class)` dédié par module. `GlobalExceptionHandler` = filet de sécurité pour l'imprévu uniquement, jamais d'exception métier spécifique dedans.

## Organisation des packages (DDD)

- Un package racine par domaine métier (`Bank`, `Caution`, `CMT`, `Leasing`, `User`, `Department`...), avec les sous-packages techniques internes (`entities`, `repositories`, `services`, `controller`, `dtos`, `mappers`, `exceptions`).
- Jamais plusieurs domaines dans un même package technique fourre-tout.
- Infrastructure transversale non métier (auth/JWT/sécurité) dans son propre package `auth`, distinct des domaines `User`/`Department` qu'elle consomme.

## Encapsulation des domaines (Bounded Context)

- Un Repository est privé à son domaine — jamais injecté ni utilisé directement par un autre domaine.
- Toute communication inter-domaines passe exclusivement par le Service du domaine consommé (le Service est l'unique point d'entrée public — façade devant Entity/Repository internes).
- Exemple : le domaine `Caution` qui a besoin d'une `Banque` injecte `BanqueService`, jamais `BanqueRepository`.
- Objectif : un domaine peut changer sa persistance (entité, repository, requêtes) sans jamais impacter les autres domaines.

## Principes de conception

- **DRY** : aucune logique métier dupliquée entre services/specifications/processors — extraire dans une méthode ou classe partagée dès la 2e occurrence.
- **SOLID** :
  - SRP : une classe = une responsabilité (Controller/Service/Repository/Mapper déjà séparés, à respecter pour tout nouveau code).
  - OCP : étendre par nouvelle implémentation (ex. `AbstractPaymentProcessor` + sous-classe par type) plutôt que modifier un comportement existant qui fonctionne.
  - LSP : une sous-classe respecte toujours le contrat de sa classe mère, sans exception cachée ni comportement surprise.
  - ISP : préférer plusieurs interfaces ciblées à une interface fourre-tout.
  - DIP : dépendre d'abstractions (interfaces `Repository`/`Service`), jamais d'implémentations concrètes injectées en dur.
- **ACID** : toute opération métier multi-étapes (validation de rapprochement, clôture, batch FunPaiement) doit être atomique via `@Transactional` — tout ou rien, jamais d'état partiel persistant si une étape échoue en cours de traitement.
- **DDD** : voir "Organisation des packages (DDD)" ci-dessus.
- **KISS** : pas de pattern ou d'abstraction additionnelle sur un module simple (CRUD basique) si rien ne le justifie.
- **YAGNI** : ne jamais ajouter un champ, une méthode ou une abstraction "au cas où" — uniquement ce qui est explicitement demandé.
- **Idempotence** : tout traitement batch/synchro (ex. FunPaiement) doit pouvoir être rejoué sans créer de doublon ni d'effet de bord — matcher sur une identité métier stable, jamais sur un insert aveugle ni sur une clé non garantie unique.
- **Fail-fast** : valider en entrée le plus tôt possible (DTO + Jakarta Validation, variables d'env sans valeur par défaut) plutôt que de laisser une donnée invalide se propager dans le système.
- **Single Source of Truth** : une règle métier, une constante ou un enum ne doit être défini qu'à un seul endroit — jamais dupliqué entre modules ou entre backend/frontend sans synchronisation explicite.

## Index sur les entités (performance)

```java
@Table(
    name = "nom_table",
    indexes = {
        @Index(name = "idx_nom_table_colonne", columnList = "colonne"),
        @Index(name = "idx_nom_table_fk_id",   columnList = "fk_id")
    }
)
```

- Toute colonne utilisée en `WHERE`/`ORDER BY` fréquent doit être indexée sur l'entité (voir `Banque` comme référence) — jamais d'index ajouté à la main en base.
- Nom d'index : `idx_<table>_<colonne>`.
- Toujours indexer : colonnes `*_id` des `@ManyToOne`/`@JoinColumn`, colonnes de statut/enum filtrées (`status`, `statut`, `type`), colonnes de date triées/filtrées par plage (`dateEmission`, `dateValeur`).
- Index composite (`columnList = "colA, colB"`) si deux colonnes sont systématiquement filtrées ensemble.
- Ne pas indexer les champs texte libre rarement filtrés (`observation`...).
- Tout nouveau critère ajouté à une `Specification` existante doit avoir son index ajouté dans le même changement.

## Audit (qui a fait quoi)

- Hibernate Envers (`@Audited`) sur toute entité sensible (mouvement d'argent, RBAC).
- `RevisionListener` custom : chaque révision Envers porte `username` (depuis `SecurityContextHolder`, fallback `"System"`/`"BATCH:<NomJob>"` pour les actions automatisées) + `timestamp`.
- Une seule table `Log`/`LogEntryDetail` (déjà existante) reste l'unique point de requête centralisé pour tout l'historique (qui, quoi, quand, quelle opération), tous domaines confondus — alimentée par un **seul** `@Aspect` générique (pointcut par convention sur `*ServiceImpl.(create|update|delete).*`), jamais un aspect par entité.
- Le détail champ-par-champ (avant/après) n'est jamais calculé ni stocké à l'écriture — il est lu à la demande depuis Envers (`AuditReader`) quand on consulte le détail d'une ligne de `Log`.
- Interdit : comparaison de champs écrite à la main dans un aspect (`compareField`/`compareAndLogFields` style) — c'est le rôle d'Envers, jamais d'un aspect custom.
- Interdit : un aspect d'audit dédié à une seule entité — toujours passer par l'aspect générique unique.

## Configuration & secrets

- Aucune valeur de configuration codée en dur dans `application.yml` ou en Java, secret ou pas — toujours `${NOM_VAR}`, jamais `${VAR:défaut}`.
- Si une variable d'environnement attendue est absente, l'application doit refuser de démarrer.
- Valeurs réelles dans `.env` (jamais commit, déjà dans `.gitignore`) ; `.env.example` tenu à jour à chaque nouvelle variable.

## Sécurité

- `/auth/login` protégé par verrouillage de compte après échecs répétés (`LoginAttemptService`) — ne jamais retirer cette protection ni la contourner pour un endpoint d'authentification.
- Pagination : toute taille de page reçue en paramètre doit être bornée — `PageableUtils.clampSize(size)` si `PageRequest` est construit à la main depuis un `@RequestParam` brut ; sinon `spring.data.web.pageable.max-page-size` s'applique déjà automatiquement (injection directe de `Pageable`).
- `/actuator/**` réservé à `SYSTEM_ADMIN`, sauf `/actuator/health` (public — requis par le healthcheck Docker, ne renvoie que UP/DOWN sans authentification).
- JWT : durée de vie volontairement courte — toute évolution vers une session plus longue doit passer par un refresh token, jamais par l'allongement du token d'accès.
- Logs : jamais logger une entité/DTO complet pouvant porter un champ sensible (mot de passe, numéro de compte, montant) — logger des identifiants (id, username) plutôt que l'objet entier.
- `ddl-auto: update` est un raccourci de dev — toute évolution de schéma destinée à la prod doit passer par un outil de migration versionné (Flyway/Liquibase) dès qu'il sera introduit, jamais par une modification manuelle de la base de prod.

## Argent (BigDecimal)

- Tout montant est un `BigDecimal` — jamais `double`/`float`.
- Comparaison de montants : toujours `compareTo`, jamais `equals` (sensible à l'échelle : `1.0` ≠ `1.00` pour `equals`).
- Arrondi : `RoundingMode` explicite à chaque division/pourcentage — jamais le défaut implicite.

