# G-Banque — Backend

API Spring Boot de gestion et de suivi des opérations bancaires internes de Staport.

> Les règles du projet sont dans `../conventions/` (point d'entrée : `../CLAUDE.md`). Ce fichier ne fait que résumer
> comment construire et lancer le backend.

## Pile technique

Java 17 · Spring Boot 3.5 · PostgreSQL 18 · Flyway · Spring Security (JWT en cookies HttpOnly) · Spring Batch ·
Hibernate Envers · MapStruct · springdoc (Swagger) · Apache POI / OpenPDF · Maven.

## Environnement unique

Aucun profil Spring : le développement est identique à la production (`../conventions/configuration.md`).
Toute la configuration vient du fichier `../.env` (modèle : `../.env.example`) ; le backend refuse de démarrer si une
variable manque.

```bash
# Depuis la racine du dépôt : toute la stack (PostgreSQL 18, backend, frontend, sauvegarde, pgAdmin),
# avec les images publiées par la CI
docker compose pull && docker compose up -d
# Essayer le code local : reconstruire l'image sous le même nom, puis relancer
docker build -t hmaydaabdo/gbanque-backend:latest . && docker compose up -d backend
```

Pour déboguer le backend depuis l'IDE contre la base de la stack : mêmes variables que `.env`, avec
`DB_HOST=localhost` et `DB_PORT=5433`.

## Base de données

Schéma géré **exclusivement** par Flyway (`src/main/resources/db/migration`), appliqué au démarrage ;
Hibernate est en `ddl-auto: validate`. Règles : `../conventions/database.md`.

## Tests

```bash
mvn verify   # tests unitaires et d'intégration (PostgreSQL 18 Testcontainers : Docker requis) + couverture JaCoCo
```

Règles : `../conventions/testing.md`. La CI rejoue la même commande (`../.github/workflows/ci.yml`).

## Points d'accès (contexte `/gbanque/api`)

| Point d'accès | Rôle |
|---|---|
| `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `GET /auth/profile` | Sessions (cookies HttpOnly + CSRF) — `../docs/rbac/README.md`, chapitre 0 |
| `/actuator/health` | Santé (public) |
| `/actuator/prometheus` | Métriques (public, pour Prometheus) |
| `/swagger-ui.html` | Documentation de l'API, si `SWAGGER_ENABLED=true` |

## Documentation

- Fonctionnelle : `../docs/modules/README.md` · Accès : `../docs/rbac/README.md`
- Exploitation : `../docs/infra/ci-cd.md`, `../docs/infra/backup-postgres.md`, `../docs/infra/migration-postgres.md`
