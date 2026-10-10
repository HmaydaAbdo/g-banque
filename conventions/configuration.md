# Configuration

> Toute valeur utilisée par l'application doit être placée dans la source de configuration appropriée.

## Principes

* Aucune règle métier configurable ne doit être codée en dur dans le code ou dans les fichiers de configuration.
* Lorsqu'une valeur hardcodée est rencontrée dans le projet, elle doit être remplacée par une configuration adaptée dans le cadre du changement en cours lorsque cela est raisonnablement possible.
* Le code contient la logique métier, jamais les paramètres métier.
* Une modification de règle métier ne doit jamais nécessiter un redéploiement de l'application.
* Toute configuration métier doit pouvoir être administrée via l'interface utilisateur.

---

## Configuration technique

Les paramètres techniques sensibles ou dépendants de l'environnement proviennent des variables d'environnement.

Exemples :

* URL ;
* ports ;
* secrets ;
* mots de passe ;
* clés API ;
* endpoints externes ;
* configuration SMTP ;
* configuration OAuth.

Toujours utiliser :

```yaml
property: ${VARIABLE}
```

Ne jamais définir de valeur par défaut.

Interdit :

```yaml
property: ${VARIABLE:defaultValue}
```

Les paramètres techniques immuables peuvent être définis dans `application.yml` lorsqu'ils ne dépendent pas de l'environnement d'exécution.

Exemples :

* `spring.jpa.hibernate.ddl-auto` ;
* configuration Jackson ;
* configuration Tomcat ;
* configuration Spring MVC ;
* niveaux de logs ;
* compression HTTP.

---

## Environnement unique (dev = prod)

* **Aucun profil Spring** (`application-dev.yml`, `application-prod.yml`, `SPRING_PROFILES_ACTIVE` : supprimés le 10/10/2026). Un seul `application.yml`, une seule configuration de logs, un seul `environment.ts` côté Angular.
* Ce qui diffère d'une machine à l'autre (hôte de base, secrets, Swagger, cookies `Secure`, origines CORS…) est **uniquement une valeur** dans le fichier `.env`, jamais un comportement codé différemment.
* `.env.example` (versionné) liste toutes les variables, sans secret. Toute nouvelle variable y est ajoutée dans le même changement, ainsi que dans `docker-compose.yml` et dans la configuration `maven-surefire-plugin` (valeur fictive pour les tests).
* Le développement utilise la **même stack Docker que la production** (`docker compose up -d --build`) : même MySQL 8, mêmes migrations Flyway, mêmes images. Pour déboguer le backend depuis l'IDE contre la base du compose : `DB_HOST=localhost`, `DB_PORT=3307`.
* Seule exception : le profil `test` (dans `src/test/resources`) coupe les jobs planifiés pendant les tests d'intégration ; il ne change ni le schéma ni la sécurité.
* Une variable inutilisée est supprimée de `.env.example`, `docker-compose.yml` et `.env` (pas de configuration morte).

---

## Configuration métier

Toute valeur métier susceptible d'évoluer doit être configurable par un administrateur via l interrface utilisateur .

Exemples :

* commissions ;
* plafonds ;
* délais ;
* pourcentages ;
* montants ;
* seuils ;
* nombre maximal de tentatives ;
* durée de verrouillage ;
* activation d'une fonctionnalité.

Ces valeurs ne doivent jamais être codées en dur dans :

* le code Java ;
* `application.yml` ;
* `application.properties`.

---

## Constantes autorisées

Peuvent rester dans le code uniquement les constantes immuables.

Exemples :

* enums ;
* codes HTTP ;
* `MediaType` ;
* expressions régulières ;
* routes REST ;
* noms de tables ;
* noms de colonnes.

---

## Interdictions

Ne jamais écrire :

```java
private static final BigDecimal COMMISSION = new BigDecimal("150");
```

```java
if (days > 30)
```

```java
loginAttempts >= 5
```

```yaml
commission.virement=150
```

---

## Lors des évolutions

Lorsqu'un développement touche une partie du code contenant des valeurs métier codées en dur, celles-ci doivent être remplacées par une configuration adaptée lorsque cela est raisonnablement possible.

Ne pas introduire de nouvelles valeurs hardcodées et ne pas perpétuer les anciennes.

---

---

## Commissions — règle spécifique

Les montants, taux et conditions de commission constituent des **paramètres métier** soumis aux règles générales de cette convention.

### Sources autorisées

| Valeur | Source |
|---|---|
| Montant d'une commission | `commission_config` (base de données) |
| Condition d'application | `CommissionCondition` (`SI_MATCH`, `SI_NON_MATCH`, `DEFAULT`) |
| Résolution du montant | `ICommissionConfigService.resolveCommission()` |

### Sources interdites

* `application.yml` (`commission.*`)
* `@Value("${commission.*}")`
* `@ConfigurationProperties(prefix = "commission.*")`
* Constantes Java (`static final BigDecimal`, `static final int`)

### Responsabilités

* La **décision de générer** une commission appartient au module source (leasing, cautions, etc.).
* La **résolution du montant** est déléguée à `ICommissionConfigService`.
* La **persistance** est déléguée à `ICommissionService`.
* L'**administration** des règles est assurée par `/portail/admin/commission-config` (SYSTEM_ADMIN uniquement).

Voir `architecture.md` — Règles 3 et 4 pour les conventions d'organisation du code.

---

## Résumé

* Le code contient la logique.
* Les variables d'environnement contiennent la configuration technique dépendante de l'environnement.
* `application.yml` contient uniquement la configuration technique immuable.
* Les paramètres métier sont configurables et administrables.
* Une règle métier ne doit jamais être codée en dur.
* Les montants de commission sont toujours résolus via `ICommissionConfigService`, jamais codés en dur.
