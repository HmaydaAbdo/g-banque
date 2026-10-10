# CI/CD et images Docker

> La CI est la porte d'entrée unique vers `main`. Aucune image n'est publiée sans être passée par elle.

---

# Cycle de vie d'un changement

1. Le travail est regroupé en **lots cohérents** (code + tests + documentation + conventions), vérifiés en local (`mvn verify`, `ng test`, build de production) avant d'être poussés.
2. **Un lot = un commit poussé sur `main` = un seul run de CI à suivre.** Pas de multiplication de branches ni de PR (la protection de `main` n'est pas disponible sur l'offre gratuite d'un dépôt privé). Une branche + PR (modèle `.github/pull_request_template.md`) n'est utilisée que pour un chantier long à faire relire avant intégration ; elle est supprimée dès la fusion (suppression automatique activée).
3. La CI s'exécute (`.github/workflows/ci.yml`, workflow unique) :
   * **backend** : `mvn verify` — tests d'intégration sur MySQL 8 Testcontainers, couverture JaCoCo, couverture du code modifié (diff-cover) ;
   * **frontend** : tests Karma + couverture, build de production ;
   * **security** : gitleaks (secrets), Trivy (dépendances, Dockerfile) ;
   * **images** : build des deux images, scan Trivy, taille affichée dans le résumé du job.
   * L'analyse statique CodeQL n'est pas disponible (dépôt privé en offre gratuite : code scanning réservé à GitHub Advanced Security).
4. Le run est suivi jusqu'au bout. **Rouge = correction immédiate**, poussée elle aussi sur `main`, avant tout autre travail.
5. Sur `main`, la CI pousse `gbanque-backend` et `gbanque-frontend` sur Docker Hub (`:latest` et `:<sha du commit>`).
6. Déploiement : manuel pour l'instant (`docker compose pull && docker compose up -d` sur le serveur) ; automatisé par SSH dans un futur `cd.yml`. Au démarrage, le backend applique les migrations Flyway en attente (cf. `conventions/database.md`).

## Recette avant déploiement

Un changement qui touche la base, la sécurité, les sessions ou une montée de version n'est déployé qu'après :

* les tests automatiques verts (CI) ;
* pour une migration de base : une **répétition** — sauvegarde de production restaurée dans un MySQL 8 jetable, backend démarré dessus (migrations appliquées, schéma validé, écrans clés vérifiés) ;
* un **parcours navigateur** des écrans concernés (connexion, rechargement, action modifiante, déconnexion), sur la stack locale ;
* la liste des actions de déploiement (variables `.env`, ordre) écrite dans `TODO.md`.

Les comptes de test créés pour une recette sont supprimés à la fin de la recette.

## Usage de l'IA

* L'IA accélère la rédaction ; elle ne remplace ni la revue, ni les tests, ni la documentation.
* Le code généré passe par la même CI et la même revue que le code écrit à la main.
* Les décisions d'architecture restent validées par un humain avant implémentation (cf. `CLAUDE.md`).
* Aucun secret, donnée client ou extrait de base de production n'est transmis à un outil d'IA.

---

# Règles de la pipeline

* **Reproductibilité** : `npm ci` (jamais `npm install`) ; `package-lock.json` versionné ; versions de base d'images épinglées au minimum en `major.minor`.
* **Un seuil bloquant ne se désactive pas pour faire passer une PR.** Un contrôle temporairement non bloquant est marqué dans le workflow (commentaire + entrée `TODO.md`) et retiré dès que possible.
* Les mises à jour de dépendances se font à la main, comme un lot validé par la CI (Dependabot volontairement non utilisé : trop de PR et de runs à suivre). Les montées de version **majeures** (Java, Spring Boot, Angular, Node) sont des chantiers décidés, jamais automatiques.
* Toute version ajoutée (action GitHub, image Docker, dépendance) est vérifiée avant usage : elle existe réellement (tag publié), elle a au moins deux semaines, et les notes de version des majeures ont été lues. Les images de `docker-compose.yml` sont épinglées (jamais `latest`) ; une image dont l'éditeur a figé `latest` est épinglée par empreinte.
* Les téléchargements Docker Hub de la CI sont authentifiés (limite des pulls anonymes).
* Fonctions GitHub indisponibles sur l'offre actuelle (dépôt privé gratuit) : protection de branche, CodeQL / code scanning. Ne pas les réintroduire sans changement d'offre.
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

* Scan Trivy de chaque image sur HIGH/CRITICAL corrigeables, bloquant pour les deux images. Toute exception est justifiée et datée dans `.trivyignore`.
* nginx : `server_tokens off`, en-têtes `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`. La CSP est à cadrer avec PrimeNG avant activation.
* Ports publiés uniquement sur `127.0.0.1` dans `docker-compose.yml` (le reverse proxy du serveur est le seul point d'entrée).
