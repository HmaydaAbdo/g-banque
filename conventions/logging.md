# Logging

> Conventions de journalisation applicables à tout le projet.

## Logger

* Déclarer un logger dans chaque classe qui produit des logs.

```java
private static final Logger log = LoggerFactory.getLogger(Xxx.class);
```

* Nom du logger : `log`.
* Ne pas utiliser Lombok (`@Slf4j`).

---

# Messages

* Toujours utiliser les messages paramétrés.

```java
log.info("Banque créée : id={}", id);
```

Ne jamais concaténer les chaînes.

```java
// ❌
log.info("Banque créée : " + id);
```

---

# Niveaux

## ERROR

Utiliser pour les erreurs non récupérables nécessitant une intervention.

Toujours logger la stacktrace.

---

## WARN

Utiliser pour un comportement anormal mais géré.

Exemples :

* tentative de connexion invalide ;
* violation de règle métier ;
* clé étrangère bloquante.

---

## INFO

Utiliser pour les événements métier importants.

Exemples :

* création ;
* modification ;
* suppression ;
* connexion réussie ;
* lancement ou fin d'un traitement métier.

---

## DEBUG

Utiliser uniquement pour le diagnostic technique.

* Désactivé sur la console de production (stdout, filtré à `INFO`). Écrit dans le fichier de logs en production, niveau réglable via `LOG_LEVEL_APP` (voir « Fichier de logs »).
* Ne jamais utiliser pour tracer les événements métier.

---

## TRACE

* Réservé au diagnostic très fin.
* Ne jamais utiliser en production.

---

# Exceptions

Logger les exceptions avec leur stacktrace.

```java
log.error("Erreur lors de la création de la banque", e);
```

Ne jamais écrire :

```java
log.error(e.getMessage());
```

Ne jamais utiliser :

```java
System.out.println(...);
System.err.println(...);
e.printStackTrace();
```

---

# Données sensibles

Ne jamais logger :

* mot de passe ;
* JWT ;
* secret ;
* numéro de compte ;
* montant financier brut ;
* entité complète ;
* DTO complet.

Logger uniquement des identifiants métier.

Exemples :

* id ;
* username ;
* référence ;
* code.

---

# Emplacement

Les logs métier sont écrits uniquement dans les Services.

Ne jamais logger dans :

* Controller ;
* Repository ;
* Mapper.

---

# MDC

`requestId` et `userId` sont ajoutés automatiquement par `MdcRequestFilter`.

* Ne jamais les renseigner manuellement.
* Tous les logs d'une requête les héritent automatiquement.

---

# Production

* Les logs passent par un `AsyncAppender`.
* Sur la console (stdout), le format est JSON via `logstash-logback-encoder`.
* Ne jamais écrire directement dans un fichier depuis le code : le fichier de logs est géré uniquement par Logback.

---

# Fichier de logs

Dev et prod écrivent, en plus de la console, dans un fichier texte lisible, **un fichier par jour** :

```text
<LOG_DIR>/gbanque-AAAA-MM-JJ.N.log
```

* `LOG_DIR` : variable d'environnement, `logs` par défaut (dev, relatif au dossier de lancement du backend), `/app/logs` dans le conteneur.
* `N` vaut `0` tant que le fichier du jour reste sous 100 Mo, puis `1`, `2`…
* Conservation : 90 jours, 5 Go maximum au total (les plus anciens sont supprimés).
* En production, le dossier est le volume Docker nommé `backend-logs` : il survit aux redéploiements. Lecture : `docker exec gbanque-backend tail -f /app/logs/gbanque-$(date +%F).0.log`.
* Format : `date heure NIVEAU [requestId] [userId] logger — message` (même pattern que la console dev).
* Niveau du code applicatif (`staport.finance.gBanque`) : `DEBUG` en dev ; en prod `DEBUG` dans le fichier uniquement, la console JSON restant à `INFO`. Variable `LOG_LEVEL_APP` (ex. `INFO`) pour réduire sans rebuild. Les frameworks (Hibernate, Spring) restent à `WARN`.
* Le dossier `logs/` n'est jamais versionné.

---

# Audit

Les logs ne remplacent jamais l'audit.

* Les logs servent au diagnostic.
* L'audit sert à tracer les actions métier.

Une opération sensible doit respecter les conventions des deux mécanismes.

---

# Principes

* Les logs doivent être utiles au diagnostic.
* Chaque événement métier important produit un seul log `INFO`.
* Les erreurs métier récupérées produisent un `WARN`.
* Les erreurs techniques produisent un `ERROR`.
* Les exceptions sont toujours journalisées avec leur stacktrace.
* Les logs ne doivent jamais exposer d'informations sensibles.
* Éviter les logs redondants ou inutiles.
