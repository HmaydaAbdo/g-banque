# Pipeline CI et publication des images — guide d'exploitation

**Audience :** développeurs / exploitation. Règles générales : `conventions/ci-cd.md` et `conventions/testing.md`.

## Ce que fait la CI aujourd'hui

| Workflow | Déclencheur | Rôle |
|---|---|---|
| `ci.yml` — job `backend` | PR, push `main` | `mvn verify` : tests d'intégration (MySQL 8 Testcontainers) + JaCoCo + couverture du code modifié |
| `ci.yml` — job `frontend` | PR, push `main` | Tests Karma + couverture (non bloquant temporairement), build de production |
| `ci.yml` — job `security` | PR, push `main` | gitleaks (secrets) + Trivy (dépendances, Dockerfile) |
| `ci.yml` — job `images` | après les trois précédents | Build des images, scan Trivy, taille ; push Docker Hub sur `main` |

Les rapports (JaCoCo, Karma, résumés de taille, couverture du code modifié) sont dans le **résumé du run** et dans les artefacts (14 jours).

## Mise en route (une seule fois)

1. **Docker Hub** : créer un *Access Token* (Account Settings → Security, droits *Read & Write*).
2. **GitHub** → dépôt → *Settings → Secrets and variables → Actions* :
   * *Variables* → `DOCKERHUB_USERNAME` = identifiant Docker Hub ;
   * *Secrets* → `DOCKERHUB_TOKEN` = le token créé.
   Sans ces deux valeurs, la CI fonctionne mais ne publie rien.
3. **Protection de `main`** : indisponible sur un dépôt privé en offre gratuite (erreur 403). Règle : un lot cohérent = un commit sur `main` = un run de CI suivi jusqu'au vert (cf. `conventions/ci-cd.md`).
4. *Settings → Code security* : activer Dependabot alerts, Secret scanning et Push protection.

## Images publiées

* `<DOCKERHUB_USERNAME>/gbanque-backend` et `<DOCKERHUB_USERNAME>/gbanque-frontend`, tags `latest` et `<sha du commit>`.
* Revenir à une version précédente = déployer le tag `<sha>` voulu.

## Vérifier localement comme la CI

```bash
# Backend (Docker requis pour Testcontainers)
cd Backend && mvn verify
# Frontend
cd Frontend && npm ci && npx ng test --watch=false --browsers=ChromeHeadlessCI --code-coverage
# Images
docker build -t gbanque-backend:test Backend && docker build -t gbanque-frontend:test Frontend
```

Docker Engine 29+ exige Testcontainers ≥ 1.21.4 (propriété `testcontainers.version` du `pom.xml`).

## Passage au déploiement continu (accès SSH au serveur)

Prévu dans un workflow `cd.yml` déclenché après `ci.yml` sur `main` : connexion SSH (clé dédiée, secret `SSH_PRIVATE_KEY`), `docker compose pull`, `docker compose up -d`, contrôle des healthchecks, retour arrière sur le tag précédent en cas d'échec. Prérequis : `docker-compose.yml` du serveur référençant les images Docker Hub (`image:` au lieu de `build:`), environnement GitHub `production` avec approbation manuelle.
