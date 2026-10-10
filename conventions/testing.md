# Tests

> Un code non testé n'est pas terminé. Les tests font partie de la Definition of Done au même titre que la documentation.

---

# Principes

* Toute création ou modification de comportement métier s'accompagne de tests, dans le même changement.
* Un test vérifie un **comportement observable** (résultat, état, erreur levée), jamais l'implémentation interne.
* Les tests sont déterministes : pas d'horloge système (`LocalDate.now()` injecté ou figé), pas d'ordre d'exécution imposé, pas de dépendance à une donnée de production.
* Un bug corrigé = un test qui reproduit le bug (écrit avant le correctif).
* Le code généré par une IA est soumis aux mêmes exigences : l'auteur de la PR relit, comprend et teste ce qu'il propose.

---

# Pyramide

| Niveau | Backend | Frontend | Quand |
|---|---|---|---|
| **Unitaire** | JUnit 5 + Mockito, sans Spring | Jasmine/Karma, `TestBed` minimal | Règles de calcul (agios, commissions, encours), mappers, validateurs, pipes, services purs |
| **Intégration** | `@SpringBootTest` / `@DataJpaTest` sur **MySQL 8 Testcontainers** | `HttpTestingController` pour les services HTTP | Repositories (requêtes natives, vues SQL), services transactionnels, sécurité (`@PreAuthorize`), audit Envers |
| **Composant** | — | `TestBed` + composant + providers de test | Écrans à logique (formulaires, tableaux filtrés, actions conditionnées aux droits) |
| **Bout en bout** | Parcours critiques via API (MockMvc / RestAssured) | Playwright (à introduire, cf. TODO) | Un parcours par chantier majeur : saisie → validation → rapprochement |

Répartition cible : beaucoup d'unitaires, une couche d'intégration ciblée, peu de bout en bout.

---

# Backend

## Emplacement et nommage

* Miroir de `src/main/java` : `src/test/java/staport/finance/gBanque/<module>/...`.
* Classe testée `XxxService` → `XxxServiceTest` (unitaire) ou `XxxIntegrationTest` (intégration, nécessite Docker ; suffixe reconnu par `maven-surefire-plugin`, contrairement à `IT`).
* Méthode : `methode_conditions_resultatAttendu` ou description en français via `@DisplayName`.

## Base de données

* **Jamais H2** : les requêtes natives, types `DECIMAL`, vues (`v_recapitulatif_encours`) et dates doivent se comporter comme en production.
* MySQL 8 jetable via Testcontainers et `@ServiceConnection`. Toute classe d'intégration **hérite de `AbstractIntegrationTest`** sans ajouter de configuration (`@MockBean`, `@TestPropertySource`, profil…) : un seul conteneur et un seul contexte Spring pour toute la suite, sinon chaque classe redémarre le contexte (plusieurs minutes). Seul Docker est requis sur le poste.
* Requêtes HTTP : `MockMvc` (`@AutoConfigureMockMvc` dans la base). Les requêtes modifiantes authentifiées par cookie envoient le cookie `XSRF-TOKEN` et l'en-tête `X-XSRF-TOKEN` (cf. `AuthSessionIntegrationTest`).
* Profil `test` (`src/test/resources/application-test.yml`) : schéma créé par Hibernate, jobs planifiés désactivés. Les variables d'environnement exigées par `application.yml` reçoivent des valeurs fictives dans la configuration `maven-surefire-plugin` du `pom.xml`.
* Les données de test sont créées par le test (builders / fixtures dédiés), jamais lues depuis la base de production ni depuis `sql/`.

## Règles

* Les calculs financiers sont testés avec des valeurs exactes (`BigDecimal`, `compareTo` ou comparaison avec échelle explicite), des cas limites (jour de prise inclus/exclu, année bissextile, changement de trimestre, montant nul) et au moins un cas tiré d'un relevé bancaire réel anonymisé.
* Chaque `@PreAuthorize` est couvert par un test « accès autorisé » et un test « accès refusé » (cf. `docs/rbac/README.md`).
* Les handlers du registre (validation, annulation, synchronisation d'édition) ont un test par source de transaction.

---

# Frontend

* Un fichier `*.spec.ts` à côté du fichier testé.
* **Interdit** de conserver un spec « should create » généré par le CLI sans les providers nécessaires : un spec qui échoue en permanence est supprimé ou réparé dans le même changement.
* Les dépendances communes (HTTP intercepté, routeur, animations, `DatePipe`, `MessageService`, `ConfirmationService`, dialogue dynamique) sont fournies par `src/testing/test-providers.ts` : `providers: testProviders` (ou `[...testProviders, <surcharges>]`, ex. `ActivatedRoute` d'un écran enfant). Tout service global ajouté à `app.config.ts` y est ajouté aussi.
* Les services HTTP sont testés avec `provideHttpClientTesting` ; les composants avec leurs dépendances remplacées par des doubles, pas avec le vrai backend.
* Les guards et conditions d'affichage liées aux droits (`isSystemAdmin()`, `departmentGuard`) sont testés (cf. `docs/rbac/README.md`).
* Exécution locale : `npx ng test --watch=false --browsers=ChromeHeadlessCI --code-coverage`.

---

# Couverture

| Périmètre | Exigence |
|---|---|
| Code ajouté/modifié dans une PR | `DIFF_COVER_MIN` dans `.github/workflows/ci.yml` (valeur actuelle : voir fichier) |
| Couverture globale | Suivie et affichée en CI, pas de seuil global tant que le socle de tests n'est pas constitué |

Trajectoire du seuil sur le code modifié : **0 → 30 → 50 → 70 → 80 %**. On passe au palier suivant lorsque la CI est restée verte deux semaines au palier courant. Le palier est relevé par une PR dédiée qui modifie `DIFF_COVER_MIN`.

Les modules financiers (agios, commissions, encours, rapprochement) visent 90 % de couverture de lignes sur les classes de calcul.

---

# Exécution

* Backend : `mvn verify` (tests + rapport JaCoCo dans `Backend/target/site/jacoco/`).
* Frontend : voir ci-dessus.
* La CI (`ci.yml`) rejoue ces commandes à l'identique ; un test qui n'est vert qu'en local est un test cassé.
* Un test lent ou instable est corrigé ou supprimé, jamais ignoré (`@Disabled` sans ticket interdit).
