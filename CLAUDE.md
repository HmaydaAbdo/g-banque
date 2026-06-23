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
- **Zéro dead code** : tout fichier, classe, méthode ou aspect qui ne sert plus doit être supprimé (`git rm`) lors du sweep du module concerné — pas commenté, pas conservé "au cas où". Un pointcut qui ne matche plus rien, un service extrait vers ailleurs, un import inutilisé : c'est du bruit, ça part. Si la suppression est techniquement impossible dans la session courante, neutraliser avec un commentaire explicite ET noter dans `TODO.md` pour suppression manuelle immédiate.

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

- Hibernate Envers (`@Audited`) sur toute entité sensible (mouvement d'argent, RBAC). Champ sensible (mot de passe...) exclu via `@NotAudited` — jamais dans l'historique, même haché.
- `AuditRevisionListener` (package `audit/`) : chaque révision Envers porte `username` + `timestamp`. `username` résolu par `CurrentUserResolver` (statique, car `RevisionListener` est instancié par Envers hors du contexte Spring) : job batch identifié (`BatchAuditContext`) > utilisateur authentifié (`SecurityContextHolder`) > `"System"`.
- **Méthode auditée = annotation explicite `@AuditableAction(operation, entity)`** posée sur la méthode de Service — jamais une convention de nommage (`create*`/`update*`/`delete*`). Une méthode sensible qui ne commence par aucun de ces verbes (`setSystemAdmin`, `assignDepartmentRole`...) ne doit jamais dépendre d'un nom pour être tracée : sans l'annotation, elle n'est juste pas auditée, et c'est visible à la revue de code — pas un trou silencieux.
  - Méthode "upsert" (crée ou met à jour selon le cas) → `operation = UPDATE` par défaut (convention).
  - Si l'ID de l'entité n'est pas déductible des paramètres (cas où l'appelant ne connaît qu'une clé composite, ex. `removeDepartmentRole(userId, departmentId)`), la méthode doit renvoyer l'ID de la ligne concernée au lieu de `void` plutôt que d'introduire un mécanisme d'extraction ad-hoc.
- Une seule table `Log`/`LogEntryDetail` (déjà existante) reste l'unique point de requête centralisé pour tout l'historique (qui, quoi, quand, quelle opération), tous domaines confondus — alimentée par un **seul** `@Aspect` générique (`GenericAuditAspect`, pointcut `@annotation(AuditableAction)`), jamais un aspect par entité.
- `GenericAuditAspect` écrit `Log` (jamais `LogEntryDetail`) après le commit de la transaction métier d'origine (`TransactionSynchronization.afterCommit`, robuste à l'ordre des proxies AOP), avec le numéro de révision Envers correspondant (`Log.revisionNumber`).
- Le détail champ-par-champ (avant/après) n'est jamais calculé ni stocké à l'écriture — il est lu à la demande depuis Envers (`AuditReader`, via `EnversDiffService`, comparaison générique par réflexion sur les champs scalaires) quand on consulte le détail d'une ligne de `Log` (`GET /logs/{id}/details`).
- Interdit : comparaison de champs écrite à la main dans un aspect (`compareField`/`compareAndLogFields` style) — c'est le rôle d'Envers (lu via `EnversDiffService`), jamais d'un aspect custom.
- Interdit : un aspect d'audit dédié à une seule entité — toujours passer par `GenericAuditAspect`.
- **Migration en cours, module par module** — les anciens aspects par entité (`LigneFonctionnelleAuditAspect`, `NatureLigneFonctionnelleAuditAspect`) restent en place et continuent de fonctionner pour leurs modules tant que ceux-ci n'ont pas été migrés vers `@Audited`/`@AuditableAction` (coexistence assumée pendant la transition — voir TODO.md). `Bank` est déjà migré (`@Audited` sur `Banque`, `@AuditableAction` sur `BanqueServiceImpl`) ; `BanqueAuditAspect` est neutralisé (son pointcut ciblait une classe renommée depuis, il était déjà inerte avant la migration). Tout nouveau module doit utiliser directement le mécanisme générique, jamais un nouvel aspect dédié.

## Configuration & secrets

- Aucune valeur de configuration codée en dur dans `application.yml` ou en Java, secret ou pas — toujours `${NOM_VAR}`, jamais `${VAR:défaut}`.
- Si une variable d'environnement attendue est absente, l'application doit refuser de démarrer.
- Valeurs réelles dans `.env` (jamais commit, déjà dans `.gitignore`) ; `.env.example` tenu à jour à chaque nouvelle variable.

## Sécurité

- `/auth/login` protégé par verrouillage de compte après échecs répétés (`LoginAttemptService`) — ne jamais retirer cette protection ni la contourner pour un endpoint d'authentification.
- Pagination : toute taille de page reçue en paramètre doit être bornée — `PageableUtils.clampSize(size)` si `PageRequest` est construit à la main depuis un `@RequestParam` brut ; sinon `spring.data.web.pageable.max-page-size` s'applique déjà automatiquement (injection directe de `Pageable`).
- `/actuator/**` réservé à `SYSTEM_ADMIN`, sauf `/actuator/health` (public — requis par le healthcheck Docker, ne renvoie que UP/DOWN sans authentification).
- JWT : durée de vie volontairement courte — toute évolution vers une session plus longue doit passer par un refresh token, jamais par l'allongement du token d'accès.
- `ddl-auto: update` est un raccourci de dev — toute évolution de schéma destinée à la prod doit passer par un outil de migration versionné (Flyway/Liquibase) dès qu'il sera introduit, jamais par une modification manuelle de la base de prod.

## Logs

- **Déclaration** : `private static final Logger log = LoggerFactory.getLogger(ClassName.class);` — pas de Lombok dans ce projet, déclaration manuelle uniquement. Nommer le champ `log` (convention SLF4J).
- **Messages paramétrés** : toujours `log.error("msg id={}", id, e)` — jamais `log.error("msg " + id, e)`. La concaténation évalue l'expression même si le niveau est désactivé.
- **Interdits** : `System.out.println`, `e.printStackTrace()`, `System.err.println` — lever une exception ou logguer avec la stack : `log.error("contexte", e)`.
- **Données sensibles** : jamais logger un mot de passe, token, numéro de compte, montant brut, entité/DTO complet pouvant porter ces champs — logger uniquement des identifiants (id, username).
- **Niveaux — règle stricte** :
  - `ERROR` : échec irrécupérable, intervention humaine requise (ex. erreur de persistence non gérée).
  - `WARN` : comportement anormal mais récupéré (ex. tentative de connexion échouée, retry réussi, blocage FK).
  - `INFO` : événements métier significatifs (création/modification/suppression d'une entité, connexion réussie).
  - `DEBUG` : diagnostic technique — désactivé en prod par défaut, ne jamais activer en prod sans raison.
  - `TRACE` : jamais en prod.
- **MDC** : `requestId` et `userId` sont injectés automatiquement par `MdcRequestFilter` sur chaque requête — ne jamais les poser manuellement dans les services ou controllers. Ces champs apparaissent dans tous les logs de la requête sans action supplémentaire.
- **Où logguer** : dans les services (couche métier), pas dans les controllers ni les repositories. Un `INFO` par opération d'écriture (create/update/delete), un `WARN` sur les cas d'erreur métier capturés (ex. verrouillage compte, FK violation).
- **Appender async** : en profil `prod`, les logs passent par un `AsyncAppender` pour ne pas bloquer le thread HTTP. Ne jamais lire/écrire un fichier de log directement depuis le code applicatif.
- **Format prod** : JSON structuré via `logstash-logback-encoder` (configuré dans `logback-spring.xml`) — chaque champ (timestamp, level, service, traceId, requestId, userId, message) est interrogeable indépendamment.

## Documentation API (Swagger / OpenAPI 3)

- **`@Tag`** : obligatoire au niveau classe controller — `name` court en français, `description` une phrase métier.
- **`@Operation`** : obligatoire sur chaque méthode de controller — `summary` ≤ 8 mots, `description` uniquement si le comportement n'est pas trivial (cas de blocage, règle métier non évidente).
- **`@ApiResponse`** : documenter les codes non-2xx pertinents uniquement — 201, 204, 400, 404, 409, 429. Ne jamais annoter 200 OK (Springdoc le génère automatiquement).
- **`@Parameter`** : pour les query params dont le nom seul n'est pas explicite (filtres, pagination) ; inutile pour les `@PathVariable` simples (`{id}`).
- **`@SecurityRequirement(name = "bearerAuth")`** : au niveau classe sur tout controller derrière JWT — ne pas le répéter sur chaque méthode.
- **DTOs** : `@Schema(description = "...")` sur les champs non évidents (enums, contraintes de format) — ne pas redoubler la contrainte Bean Validation déjà présente.
- **`OpenApiConfig`** : une seule classe de configuration centralisée déclare le `SecurityScheme` JWT (`bearerAuth`). Ne jamais redéfinir le scheme dans les controllers.
- **Périmètre** : annoter uniquement la couche controller et les DTOs. Jamais les services, repositories, filtres ou aspects.
- **Sécurité Swagger** : `/swagger-ui/**` et `/v3/api-docs/**` sont en `permitAll()` dans `SecurityConfig` (la page est statique, l'auth se fait via le bouton "Authorize" de l'UI). Le verrou réel est `springdoc.swagger-ui.enabled: ${SWAGGER_ENABLED}` dans `application.yml` — `SWAGGER_ENABLED=false` en prod, `true` en dev. Ne jamais retirer le `permitAll()` Swagger de `SecurityConfig` (casse l'UI) ni mettre `enabled: true` en dur.

## Documentation de module — règle de mise à jour

**Après chaque sweep de module, mettre à jour les 3 niveaux de documentation avant de passer au module suivant :**

1. `docs/modules/<nom>.md` — fiche technique (backend + frontend) : entités, endpoints, RBAC, audit, architecture interne.
2. `docs/direction/<nom>.md` — présentation fonctionnelle 1-2 pages : ce que fait le module pour le métier, qui l'utilise, valeur ajoutée, captures d'écran si disponibles.
3. `docs/guide-utilisateur/<profil>.md` — mettre à jour la section concernée dans le guide du profil qui utilise ce module (`admin.md` si SYSTEM_ADMIN, `finance.md` si Finance, les deux si partagé).

Ces 3 fichiers sont la source de vérité documentaire du projet. Ne jamais terminer un sweep sans les avoir tous mis à jour.

## Documentation de module (`docs/modules/<nom>.md`)

Chaque fiche de module couvre **backend et frontend** — elle sert à la fois de référence technique, de base pour la présentation fonctionnelle à la direction, et de source pour le futur guide utilisateur. Structure obligatoire :

1. **Rôle** — ce que fait le module pour le métier (2-3 phrases, sans jargon technique), puis sa place dans l'architecture (package, dépendances).
2. **Entité(s)** — tableau des champs JPA + indexes. Omettre si le module n'a pas d'entité propre (ex. `auth`).
3. **Interface utilisateur** — vues Angular : une ligne par écran (route, composant, description de ce que l'utilisateur voit et peut faire), plus la navigation (sidebar, breadcrumb, redirections).
4. **Endpoints** — tableau `Méthode | Route | Accès | Description`.
5. **Règles métier** — invariants, validations, blocages, comportements non triviaux visibles par l'utilisateur.
6. **Architecture interne** — décisions de conception cross-domaines (encapsulation, events, services dédiés). Omettre si le module est un CRUD simple sans décision notable.
7. **Audit** — couverture Envers (`@Audited`) + `@AuditableAction`, champs exclus et raison.
8. **Qui peut faire quoi** — RBAC complet : `@PreAuthorize` backend (méthodes concernées), guard Angular (route parente ou enfant), sidebar (`requiredAuthority`).

## Métriques Micrometer — règle de sweep

**Lors du sweep de chaque module, ajouter les métriques Micrometer dans le même changement.**

### Structure
Créer une classe `XxxMetrics` dans un sous-package `metrics/` du domaine :
```
module/
  metrics/
    XxxMetrics.java   ← @Component, injecte MeterRegistry
  service/
    XxxServiceImpl.java ← injecte XxxMetrics
```

### Convention de nommage
`gbanque.<module>.<objet>.<mesure>` — tout en minuscules, séparateur `.`
Exemples :
- `gbanque.rapprochement.en_cours.count`
- `gbanque.caution.active.count`
- `gbanque.batch.funpaiement.operations.total`
- `gbanque.batch.funpaiement.job.duration`

### Types à utiliser
- **Gauge** : valeur instantanée interrogée à chaque scrape Prometheus (nb d'entités dans un état donné). Brancher sur un `Supplier<Number>` qui appelle le repository. Ne jamais incrémenter/décrémenter manuellement un Gauge.
- **Counter** : valeur qui monte uniquement (nb d'opérations réalisées, nb d'erreurs). Incrémenter dans le service après chaque opération réussie ou échouée.
- **Timer** : durée d'une opération (jobs batch, appels externes). Utiliser `Timer.record(Runnable)` ou `Timer.start()` / `stop()`.

### Où appeler
- **Gauge** : enregistré une seule fois au démarrage du bean (`@PostConstruct` ou constructeur de `XxxMetrics`). Le `Supplier` est appelé automatiquement par Prometheus à chaque scrape.
- **Counter / Timer** : appelés depuis `XxxServiceImpl` après l'opération métier (dans le bloc `try` ou après le commit).

### Ce qui ne génère pas de métriques custom
Les modules CRUD purs sans état métier significatif (Auth, User, Department) — les métriques HTTP automatiques de Micrometer suffisent (temps de réponse, codes HTTP par endpoint).

### Modules avec métriques attendues (à faire lors du sweep)
- **Rapprochement** — Gauge : nb par statut (EN_COURS, CLOTURE_PROVISOIRE, CLOTURE_DEFINITIVE) par banque
- **Caution** — Gauge : nb actives, nb expirées non levées
- **Leasing** — Gauge : nb échéances à venir J-7 / J-30
- **CMT** — Gauge : nb échéances à venir J-7 / J-30
- **Batch FunPaiement** — Counter : opérations sync OK/KO ; Timer : durée job
- **Moyens de paiement** — Counter : créations par type (LCN, virement, chèque), passages à l'état ECHU

## Argent (BigDecimal)

- Tout montant est un `BigDecimal` — jamais `double`/`float`.
- Comparaison de montants : toujours `compareTo`, jamais `equals` (sensible à l'échelle : `1.0` ≠ `1.00` pour `equals`).
- Arrondi : `RoundingMode` explicite à chaque division/pourcentage — jamais le défaut implicite.

