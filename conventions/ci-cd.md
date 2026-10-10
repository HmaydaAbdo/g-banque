# CI/CD et images Docker

> La CI est la porte d'entrée unique vers `main`. Aucune image n'est publiée sans être passée par elle.

---

# Cycle de vie d'un changement

1. Branche courte depuis `main` (`feat/…`, `fix/…`), jamais de commit direct sur `main`.
2. Pull request avec le modèle `.github/pull_request_template.md` (Definition of Done : compilation, tests, docs, conventions, pas de secret, code IA relu).
3. La CI s'exécute (`.github/workflows/ci.yml`) :
   * **backend** : `mvn verify` — tests d'intégration sur MySQL 8 Testcontainers, couverture JaCoCo, couverture du code modifié (diff-cover) ;
   * **frontend** : tests Karma + couverture, build de production ;
   * **security** : gitleaks (secrets), Trivy (dépendances, Dockerfile) ;
   * **images** : build des deux images, scan Trivy, taille affichée dans le résumé du job.
   * CodeQL (job `codeql` de `ci.yml`) analyse le code Java/TypeScript à chaque PR et chaque push sur `main`.
4. Revue humaine, puis merge (squash) dans `main`.
5. Sur `main`, la CI pousse `gbanque-backend` et `gbanque-frontend` sur Docker Hub (`:latest` et `:<sha du commit>`).
6. Déploiement : manuel pour l'instant (`docker compose pull && docker compose up -d` sur le serveur) ; automatisé par SSH dans un futur `cd.yml`.

## Usage de l'IA

* L'IA accélère la rédaction ; elle ne remplace ni la revue, ni les tests, ni la documentation.
* Le code généré passe par la même CI et la même revue que le code écrit à la main.
* Les décisions d'architecture restent validées par un humain avant implémentation (cf. `CLAUDE.md`).
* Aucun secret, donnée client ou extrait de base de production n'est transmis à un outil d'IA.

---

# Règles de la pipeline

* **Reproductibilité** : `npm ci` (jamais `npm install`) ; `package-lock.json` versionné ; versions de base d'images épinglées au minimum en `major.minor`.
* **Un seuil bloquant ne se désactive pas pour faire passer une PR.** Un contrôle temporairement non bloquant est marqué dans le workflow (commentaire + entrée `TODO.md`) et retiré dès que possible.
* Les mises à jour de dépendances se font à la main, par une PR dédiée validée par la CI (Dependabot volontairement non utilisé : trop de PR et de runs à suivre).
* Les secrets (`DOCKERHUB_TOKEN`, futurs accès SSH) sont des *secrets* GitHub ; l'identifiant Docker Hub est la variable de dépôt `DOCKERHUB_USERNAME`. Jamais de secret dans le dépôt ni dans les logs.

---

# Images Docker

## Obligatoire

* Build multi-étapes : le runtime ne contient ni compilateur, ni outils de build, ni sources.
* Utilisateur **non-root** dans l'image finale.
* `HEALTHCHECK` sans outil supplémentaire (`wget` de busybox), cohérent avec `docker-compose.yml`.
* `apk upgrade --no-cache` dans l'étape runtime pour embarquer les correctifs de l'OS.
* `.dockerignore` strict (pas de `target/`, `node_modules/`, tests, `.env`).
* Aucun secret dans l'image ni dans les `ARG`/`ENV` : la configuration passe par les variables d'environnement de `docker-compose.yml`.

## Réduction de taille

* **Backend** : JAR Spring Boot extrait par couches (`layertools`) — dépendances / loader / application. Un redéploiement ne retransfère que la couche `application` (~5 Mo). Image runtime `eclipse-temurin:17-jre-alpine`.
* **Frontend** : `nginx:alpine-slim`, uniquement les fichiers statiques du build de production.
* Cache de build : montages `--mount=type=cache` (Maven, npm) et cache GitHub Actions (`type=gha`).

## Sécurité

* Scan Trivy de chaque image sur HIGH/CRITICAL corrigeables. Frontend : bloquant. Backend : rapport seul tant que la montée de version Spring Boot n'est pas faite (cf. `TODO.md`), puis bloquant.
* nginx : `server_tokens off`, en-têtes `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`. La CSP est à cadrer avec PrimeNG avant activation.
* Ports publiés uniquement sur `127.0.0.1` dans `docker-compose.yml` (le reverse proxy du serveur est le seul point d'entrée).
