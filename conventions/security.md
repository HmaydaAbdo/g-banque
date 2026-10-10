# Security

> Conventions de sécurité applicables à tout le projet.

# Authentification

## JWT

* Authentification par JWT.
* Les Access Tokens ont une durée de vie courte (`JWT_ACCESS_TOKEN_MINUTES`, 15 min).
* Une session plus longue doit utiliser un Refresh Token, jamais un Access Token plus long (`JWT_REFRESH_TOKEN_HOURS`, 8 h, durée maximale de session non prolongeable).

## Stockage des jetons (navigateur)

* Les jetons voyagent **uniquement** en cookies `HttpOnly`, `SameSite=Strict`, posés par le backend (`AuthCookieService`). L'attribut `Secure` suit `AUTH_COOKIE_SECURE` : `true` obligatoire dès que G-Banque est servi en HTTPS ; `false` tant qu'il l'est en HTTP (un navigateur refuse un cookie `Secure` reçu en HTTP). Jamais dans `localStorage`/`sessionStorage`, jamais dans le corps d'une réponse, jamais décodés côté frontend.
* Le frontend connaît l'utilisateur par le profil renvoyé par `/auth/login`, `/auth/refresh` et `/auth/profile` (`username`, `authorities`).
* Refresh Token : opaque, seul son SHA-256 est stocké (`refresh_token`), rotation à chaque usage, réutilisation détectée ⇒ révocation de toute la session. Déconnexion = révocation côté serveur.
* Frontend et API sont servis sur la **même origine** (prod : nginx ; dev : proxy `ng serve`, `proxy.conf.json`). Ne jamais réintroduire d'URL d'API absolue : les cookies `SameSite=Strict` et l'en-tête CSRF d'Angular ne fonctionnent qu'en même origine.

## CSRF

* Protection CSRF active (authentification par cookie) : jeton double-submit `XSRF-TOKEN` / `X-XSRF-TOKEN`, géré nativement par Angular. Ne jamais désactiver `csrf` dans `SecurityConfig`.
* Seules exemptions : `/auth/login` et les requêtes portant un en-tête `Authorization: Bearer` (secours pour Swagger et outils).

---

# Autorisation

* Tout endpoint protégé utilise `@PreAuthorize` (au niveau de la classe ou de la méthode).
* Appliquer le principe du moindre privilège.
* Toute nouvelle fonctionnalité doit définir explicitement les rôles ou permissions autorisés.
* Refuser par défaut tout accès non explicitement autorisé.
* La documentation RBAC est centralisée dans `docs/rbac/README.md`, avec un chapitre par module. Chaque chapitre trace de façon exhaustive les accès du module (endpoints + `@PreAuthorize`, guards Angular, visibilité sidebar). Voir `conventions/documentation.md`, section "Documentation RBAC". Toute évolution des autorisations d'un module met à jour le chapitre correspondant dans le même changement.

---

## Suppression

La suppression d'une ressource est réservée exclusivement au `SYSTEM_ADMIN`.

Cette règle s'applique à tous les modules, sans exception.

Backend :

```java
private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

@DeleteMapping("/{id}")
@PreAuthorize(SYSTEM_ADMIN)
public ResponseEntity<Void> deleteXxx(@PathVariable Long id) { ... }
```

Frontend — le bouton de suppression est conditionné à `isSystemAdmin()` :

```html
@if (isSystemAdmin()) {
  <button pButton icon="pi pi-trash" (click)="deleteXxx(item)"></button>
}
```

```ts
isSystemAdmin(): boolean {
  return this.authService.isSystemAdmin();
}
```

---

# Authentification utilisateur

## Login

* `/auth/login` est protégé par `LoginAttemptService`.
* Ne jamais supprimer ni contourner le verrouillage après plusieurs tentatives échouées.
* Toute évolution de l'authentification doit préserver cette protection.

---

# Configuration

## Variables d'environnement

* Les paramètres dépendant de l'environnement utilisent exclusivement des variables d'environnement.

Toujours utiliser :

```yaml
property: ${VARIABLE}
```

* Ne jamais utiliser `${VARIABLE:default}`.
* Si une variable obligatoire est absente, l'application doit refuser de démarrer.

---

## Secrets

Ne jamais stocker dans le code :

* mot de passe ;
* secret JWT ;
* clé API ;
* certificat ;
* token d'accès.

Les secrets sont stockés dans :

* `.env` en développement ;
* les variables d'environnement en production.

`.env` ne doit jamais être versionné.

`.env.example` doit être maintenu à jour.

---

# Pagination

Toute pagination doit être bornée.

Si `PageRequest` est construit manuellement :

```java
PageableUtils.clampSize(size)
```

Si un `Pageable` est injecté directement :

```properties
spring.data.web.pageable.max-page-size
```

---

# Actuator

* `/actuator/health` est public.
* `/actuator/prometheus` est public — accès requis par le scraper Prometheus (pas d'authentification possible dans ce contexte). Ce point de terminaison n'expose que des métriques techniques agrégées, sans données sensibles.
* Tous les autres endpoints `/actuator/**` sont réservés à `SYSTEM_ADMIN`.
* Ne jamais exposer d'informations sensibles via Actuator.

---

# Base de données

* Le schéma est géré exclusivement par les migrations Flyway (`db/migration`) ; Hibernate est en `ddl-auto: validate`. Voir `conventions/database.md`.
* Ne jamais modifier manuellement la structure ou les données de la base de production : toute évolution passe par une migration versionnée, relue et testée.

---

# Validation

* Toute entrée utilisateur est validée avec Jakarta Validation.
* Les validations métier complémentaires sont réalisées dans les Services.
* Une donnée invalide doit être rejetée le plus tôt possible (Fail Fast).

---

# Principes

* Aucun secret dans le code source.
* Aucune information sensible dans les messages d'erreur.
* Aucun contournement des mécanismes de sécurité pour simplifier un développement.
* Toute nouvelle fonctionnalité respecte les conventions de sécurité existantes.
* La sécurité est une responsabilité de chaque développement.