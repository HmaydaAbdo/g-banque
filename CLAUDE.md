# G-Banque — Conventions Backend 


> Document de référence pour toute nouvelle fonctionnalité backend sur ce projet. Lu automatiquement par Claude au démarrage de chaque session — fait foi sur les conventions à respecter, indépendamment du module traité.

## Pattern d'architecture obligatoire (par module)

Chaque module métier (Bank, Caution, CMT, Leasing, RBAC User/Department, etc.) suit cette structure stricte :

1. **Entity** — entité JPA, jamais exposée directement dans une réponse API.
2. **Repository** — `JpaRepository` (+ `JpaSpecificationExecutor` si recherche/filtre dynamique). Toute relation chargée pour la sérialisation JSON utilise `@EntityGraph(attributePaths = {...})` plutôt que de compter sur `spring.jpa.open-in-view` — objectif : éviter les N+1, performance.
3. **Specification** — classe statique `XSpecification.searchByCriteria(XSearchCriteria)` pour toute liste filtrable/paginée (voir `BanqueSpecification`, `UserSpecification`).
4. **Mapper** — interface MapStruct (`@Mapper(componentModel = "spring")`). Les modules historiques (Bank, Caution...) utilisent encore un mapper manuel (`@Component` avec `toEntity`/`toDto` écrits à la main) — aucune migration rétroactive prévue pour l'instant, mais **tout nouveau module utilise MapStruct**.
5. **DTO** — toujours des **records** Java avec validation Jakarta complète (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`...). Request et Response **séparés** — jamais l'entité JPA en retour d'API.
6. **Service** — `@Transactional` sur toute méthode mutante (create/update/delete). Lookup interne d'entité dans une méthode privée (ex. `findEntityById`), jamais exposée hors du service.
7. **Controller** — "thin" : reçoit/retourne des DTOs uniquement, délègue tout le mapping au service. `@PreAuthorize` sur chaque endpoint ou au niveau classe selon la granularité voulue.
8. **ExceptionHandler dédié par module** — `@RestControllerAdvice(assignableTypes = XController.class)`. Jamais d'exception métier spécifique à un module dans le `GlobalExceptionHandler` partagé — celui-ci reste un filet de sécurité pour l'imprévu uniquement.

## Organisation des packages (DDD)

Chaque domaine métier vit dans son propre package racine (`Bank`, `Caution`, `CMT`, `Leasing`, `User`, `Department`...), avec les sous-packages techniques du pattern ci-dessus à l'intérieur (`entities`, `repositories`, `services`, `controller`, `dtos`, `mappers`, `exceptions`...). Jamais plusieurs domaines mélangés dans un seul package technique fourre-tout. L'infrastructure transversale non métier (auth/JWT/filtres de sécurité) vit dans son propre package (`auth`), distinct des domaines `User`/`Department` qu'elle consomme.
